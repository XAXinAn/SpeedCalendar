package com.example.speedcalendar.data.model

/**
 * AI聊天消息请求
 */
data class ChatMessageRequest(
    val message: String,
    val sessionId: String? = null,
    val userId: String,
    val context: Map<String, Any>? = null
)

/**
 * AI聊天消息响应
 */
data class ChatMessageResponse(
    val sessionId: String,
    val message: String,
    val timestamp: Long,
    val toolCalls: List<ToolCall>? = null
)

/**
 * 工具调用信息
 */
data class ToolCall(
    val toolName: String,
    val parameters: Map<String, Any>,
    val result: String? = null
)

/**
 * 聊天会话创建请求
 */
data class CreateSessionRequest(
    val userId: String,
    val title: String? = null
)

/**
 * 聊天会话响应
 */
data class ChatSessionResponse(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 聊天历史请求
 */
data class ChatHistoryRequest(
    val sessionId: String,
    val page: Int = 0,
    val size: Int = 50
)

/**
 * 聊天历史响应
 */
data class ChatHistoryResponse(
    val messages: List<ChatHistoryMessage>,
    val totalPages: Int,
    val totalElements: Int
)

/**
 * 历史消息
 */
data class ChatHistoryMessage(
    val id: String,
    val content: String,
    val role: String, // "user" 或 "ai"
    val timestamp: Long,
    val toolCalls: List<ToolCall>? = null
)

/**
 * 获取用户会话列表请求
 */
data class UserSessionsRequest(
    val userId: String,
    val page: Int = 0,
    val size: Int = 20
)

/**
 * 用户会话列表响应
 */
data class UserSessionsResponse(
    val sessions: List<ChatSessionResponse>,
    val totalPages: Int,
    val totalElements: Int
)

/**
 * 删除会话请求
 */
data class DeleteSessionRequest(
    val sessionId: String,
    val userId: String
)
