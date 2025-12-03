package com.example.speedcalendar.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.model.ActivityMessage
import com.example.speedcalendar.data.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 消息中心 ViewModel
 */
class MessageCenterViewModel : ViewModel() {

    private val repository = ActivityRepository()
    private val TAG = "MessageCenterViewModel"

    // 消息列表
    private val _messages = MutableStateFlow<List<ActivityMessage>>(emptyList())
    val messages: StateFlow<List<ActivityMessage>> = _messages.asStateFlow()

    // 未读消息数量
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 刷新状态
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 分页
    private var currentPage = 1
    private var hasMoreData = true
    private val pageSize = 20

    init {
        fetchUnreadCount()
    }

    /**
     * 加载消息列表（首次加载或刷新）
     */
    fun loadMessages(refresh: Boolean = false) {
        if (_isLoading.value || (_isRefreshing.value && !refresh)) return

        viewModelScope.launch {
            try {
                if (refresh) {
                    _isRefreshing.value = true
                    currentPage = 1
                    hasMoreData = true
                } else {
                    _isLoading.value = true
                }

                val result = repository.getMessages(currentPage, pageSize)
                result.fold(
                    onSuccess = { response ->
                        val newMessages = response.messages
                        if (refresh) {
                            _messages.value = newMessages
                        } else {
                            _messages.value = _messages.value + newMessages
                        }
                        hasMoreData = newMessages.size >= pageSize
                        _error.value = null
                    },
                    onFailure = { e ->
                        Log.e(TAG, "加载消息失败", e)
                        _error.value = e.message ?: "加载消息失败"
                    }
                )
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    /**
     * 加载更多消息
     */
    fun loadMoreMessages() {
        if (!hasMoreData || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentPage++
                val result = repository.getMessages(currentPage, pageSize)
                result.fold(
                    onSuccess = { response ->
                        val newMessages = response.messages
                        _messages.value = _messages.value + newMessages
                        hasMoreData = newMessages.size >= pageSize
                        _error.value = null
                    },
                    onFailure = { e ->
                        Log.e(TAG, "加载更多消息失败", e)
                        currentPage-- // 回退页码
                        _error.value = e.message ?: "加载更多失败"
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 获取未读消息数量
     */
    fun fetchUnreadCount() {
        viewModelScope.launch {
            val result = repository.getUnreadCount()
            result.fold(
                onSuccess = { count ->
                    _unreadCount.value = count
                },
                onFailure = { e ->
                    Log.e(TAG, "获取未读数量失败", e)
                }
            )
        }
    }

    /**
     * 标记单条消息已读
     */
    fun markAsRead(messageId: String) {
        viewModelScope.launch {
            val result = repository.markAsRead(messageId)
            result.fold(
                onSuccess = {
                    // 更新本地状态
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == messageId) msg.copy(isRead = true) else msg
                    }
                    // 减少未读数量
                    if (_unreadCount.value > 0) {
                        _unreadCount.value--
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "标记已读失败", e)
                }
            )
        }
    }

    /**
     * 标记全部已读
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            val result = repository.markAllAsRead()
            result.fold(
                onSuccess = { markedCount ->
                    // 更新本地状态
                    _messages.value = _messages.value.map { msg ->
                        msg.copy(isRead = true)
                    }
                    _unreadCount.value = 0
                    Log.d(TAG, "已标记 $markedCount 条消息为已读")
                },
                onFailure = { e ->
                    Log.e(TAG, "标记全部已读失败", e)
                    _error.value = e.message ?: "标记全部已读失败"
                }
            )
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }
}
