package com.example.speedcalendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.model.ChatMessageRequest
import com.example.speedcalendar.data.model.CreateSessionRequest
import com.example.speedcalendar.features.ai.chat.Message
import com.example.speedcalendar.features.ai.chat.MessageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatSession(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timestamp: Long
)

class AIChatViewModel : ViewModel() {

    private val apiService = RetrofitClient.aiChatApiService

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSessions(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getChatSessions(userId)
                if (response.isSuccessful && response.body()?.code == 200) {
                    _sessions.value = response.body()?.data ?: emptyList()
                } else {
                    _error.value = response.body()?.message ?: "加载会话列表失败"
                }
            } catch (e: Exception) {
                _error.value = "加载会话列表异常: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(content: String, userId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val userMessage = Message(content = content, role = MessageRole.USER)
                _messages.value = _messages.value + userMessage

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
                        if (_currentSessionId.value == null) {
                            _currentSessionId.value = data.sessionId
                            loadSessions(userId) // 如果是新会话，刷新侧边栏
                        }
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

    fun createNewSession(userId: String) {
        _currentSessionId.value = null
        _messages.value = emptyList()
        loadSessions(userId) // 重新加载会话以确保列表最新
    }

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

    fun clearError() {
        _error.value = null
    }

    fun resetSession() {
        _currentSessionId.value = null
        _messages.value = emptyList()
    }
}
