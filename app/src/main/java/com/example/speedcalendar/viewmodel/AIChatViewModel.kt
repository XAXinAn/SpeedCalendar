package com.example.speedcalendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.model.*
import com.example.speedcalendar.features.ai.chat.Message
import com.example.speedcalendar.features.ai.chat.MessageRole
import com.example.speedcalendar.features.ai.chat.ChatSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AI聊天ViewModel
 */
class AIChatViewModel : ViewModel() {

    private val apiService = RetrofitClient.aiChatApiService

    // 当前会话ID
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    // 消息列表
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // 会话列表
    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 发送消息
     */
    fun sendMessage(content: String, userId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // 添加用户消息到UI
                val userMessage = Message(
                    content = content,
                    role = MessageRole.USER
                )
                _messages.value = _messages.value + userMessage

                // 调用API
                val request = ChatMessageRequest(
                    message = content,
                    sessionId = _currentSessionId.value,
                    userId = userId
                )

                val response = apiService.sendMessage(request)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200 && apiResponse.data != null) {
                        val data = apiResponse.data

                        // 更新会话ID
                        if (_currentSessionId.value == null) {
                            _currentSessionId.value = data.sessionId
                        }

                        // 添加AI回复到UI
                        val aiMessage = Message(
                            content = data.message,
                            role = MessageRole.AI,
                            timestamp = data.timestamp
                        )
                        _messages.value = _messages.value + aiMessage

                        onSuccess?.invoke()
                    } else {
                        _error.value = apiResponse?.message ?: "发送消息失败"
                    }
                } else {
                    _error.value = "网络请求失败: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "发送消息异常: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 创建新会话
     */
    fun createNewSession(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val request = CreateSessionRequest(userId = userId)
                val response = apiService.createSession(request)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200 && apiResponse.data != null) {
                        _currentSessionId.value = apiResponse.data.id
                        _messages.value = emptyList()
                    } else {
                        _error.value = apiResponse?.message ?: "创建会话失败"
                    }
                } else {
                    _error.value = "网络请求失败: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "创建会话异常: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载聊天历史
     */
    fun loadChatHistory(sessionId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.getChatHistory(sessionId)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200 && apiResponse.data != null) {
                        val history = apiResponse.data.messages.map { historyMsg ->
                            Message(
                                id = historyMsg.id,
                                content = historyMsg.content,
                                role = if (historyMsg.role == "user") MessageRole.USER else MessageRole.AI,
                                timestamp = historyMsg.timestamp
                            )
                        }
                        _messages.value = history
                        _currentSessionId.value = sessionId
                    } else {
                        _error.value = apiResponse?.message ?: "加载历史失败"
                    }
                } else {
                    _error.value = "网络请求失败: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "加载历史异常: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载用户会话列表
     */
    fun loadUserSessions(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.getUserSessions(userId)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200 && apiResponse.data != null) {
                        val sessionList = apiResponse.data.sessions.map { session ->
                            ChatSession(
                                id = session.id,
                                title = session.title,
                                timestamp = session.updatedAt
                            )
                        }
                        _sessions.value = sessionList
                    } else {
                        _error.value = apiResponse?.message ?: "加载会话列表失败"
                    }
                } else {
                    _error.value = "网络请求失败: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "加载会话列表异常: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 删除会话
     */
    fun deleteSession(sessionId: String, userId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.deleteSession(sessionId, userId)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200) {
                        // 从列表中移除已删除的会话
                        _sessions.value = _sessions.value.filter { it.id != sessionId }

                        // 如果删除的是当前会话，清空消息
                        if (_currentSessionId.value == sessionId) {
                            _currentSessionId.value = null
                            _messages.value = emptyList()
                        }

                        onSuccess?.invoke()
                    } else {
                        _error.value = apiResponse?.message ?: "删除会话失败"
                    }
                } else {
                    _error.value = "网络请求失败: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "删除会话异常: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 重置会话（新建空白会话）
     */
    fun resetSession() {
        _currentSessionId.value = null
        _messages.value = emptyList()
    }
}
