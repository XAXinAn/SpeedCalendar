package com.example.speedcalendar.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.local.UserPreferences
import com.example.speedcalendar.data.model.ChatMessageRequest
import com.example.speedcalendar.features.ai.chat.Message
import com.example.speedcalendar.features.ai.chat.MessageRole
import com.example.speedcalendar.utils.OcrHelper
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

class AIChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AIChatViewModel"
    }

    private val apiService = RetrofitClient.aiChatApiService
    private val userPreferences = UserPreferences.getInstance(application)
    private val ocrHelper = OcrHelper.getInstance(application)

    /**
     * 获取带有 Bearer 前缀的 Authorization Token
     */
    private fun getAuthToken(): String? {
        val token = userPreferences.getAccessToken()
        val isLoggedIn = userPreferences.isLoggedIn()
        Log.d(TAG, "getAuthToken: token=${if (token != null) "存在(${token.take(20)}...)" else "null"}, isLoggedIn=$isLoggedIn")
        return if (token != null) "Bearer $token" else null
    }

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

    // OCR 相关状态
    private val _isOcrProcessing = MutableStateFlow(false)
    val isOcrProcessing: StateFlow<Boolean> = _isOcrProcessing.asStateFlow()

    private val _isOcrReady = MutableStateFlow(false)
    val isOcrReady: StateFlow<Boolean> = _isOcrReady.asStateFlow()

    private val _ocrResult = MutableStateFlow<String?>(null)
    val ocrResult: StateFlow<String?> = _ocrResult.asStateFlow()

    /**
     * 初始化 OCR 引擎（进入页面时调用）
     */
    fun initOcr() {
        viewModelScope.launch {
            val result = ocrHelper.initialize()
            _isOcrReady.value = result
            if (!result) {
                _error.value = "OCR 引擎初始化失败"
            }
        }
    }

    /**
     * 处理图片进行 OCR 识别
     * @param uri 图片 Uri
     * @param onResult 识别完成回调，返回格式化后的文本（带"帮我添加日程："前缀）
     */
    fun processImageForOcr(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _isOcrProcessing.value = true
            _error.value = null
            
            try {
                val result = ocrHelper.recognizeFromUri(uri)
                if (result.success) {
                    _ocrResult.value = result.formattedText
                    onResult(result.formattedText)
                } else {
                    _error.value = result.error ?: "OCR 识别失败"
                }
            } catch (e: Exception) {
                _error.value = "OCR 识别异常: ${e.message}"
            } finally {
                _isOcrProcessing.value = false
            }
        }
    }

    /**
     * 清除 OCR 结果
     */
    fun clearOcrResult() {
        _ocrResult.value = null
    }

    /**
     * 加载会话列表
     * userId 从 token 中自动获取，不需要传递
     */
    fun loadSessions() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // token 由 AuthInterceptor 自动添加
                Log.d(TAG, "loadSessions: 发送请求")
                val response = apiService.getChatSessions()
                val body = response.body()
                Log.d(TAG, "loadSessions: HTTP=${response.code()}, body.code=${body?.code}")
                
                if (response.isSuccessful && body != null) {
                    when (body.code) {
                        200 -> _sessions.value = body.data ?: emptyList()
                        401 -> _error.value = "登录已过期，请重新登录"
                        else -> _error.value = body.message ?: "加载会话列表失败"
                    }
                } else {
                    _error.value = "网络请求失败 (${response.code()})"
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadSessions: 异常", e)
                _error.value = "加载会话列表异常: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 发送消息
     * userId 从 token 中自动获取，不需要传递
     */
    fun sendMessage(content: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // token 由 AuthInterceptor 自动添加

                val userMessage = Message(content = content, role = MessageRole.USER)
                _messages.value = _messages.value + userMessage

                val request = ChatMessageRequest(
                    message = content,
                    sessionId = _currentSessionId.value
                )

                val response = apiService.sendMessage(request)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200 && apiResponse.data != null) {
                        val data = apiResponse.data
                        if (_currentSessionId.value == null) {
                            _currentSessionId.value = data.sessionId
                            loadSessions() // 如果是新会话，刷新侧边栏
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

    /**
     * 创建新会话
     */
    fun createNewSession() {
        _currentSessionId.value = null
        _messages.value = emptyList()
        loadSessions() // 重新加载会话以确保列表最新
    }

    fun loadChatHistory(sessionId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // token 由 AuthInterceptor 自动添加

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
