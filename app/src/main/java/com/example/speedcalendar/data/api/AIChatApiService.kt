package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.*
import com.example.speedcalendar.viewmodel.ChatSession
import retrofit2.Response
import retrofit2.http.*

/**
 * AI聊天API服务
 * 注意：所有接口都需要 Authorization 请求头，userId 从 token 解析
 */
interface AIChatApiService {

    /**
     * 发送聊天消息
     * POST /api/ai/chat/message
     * 请求体: { message: string, sessionId?: string }
     * 响应: { code: 200, data: { sessionId, message, timestamp } }
     */
    @POST("ai/chat/message")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Body request: ChatMessageRequest
    ): Response<ApiResponse<ChatMessageResponse>>

    /**
     * 创建新的聊天会话
     * POST /api/ai/chat/sessions
     * 请求体: {} (空对象或不传)
     */
    @POST("ai/chat/sessions")
    suspend fun createSession(
        @Header("Authorization") token: String
    ): Response<ApiResponse<ChatSessionResponse>>

    /**
     * 获取聊天历史
     * GET /api/ai/chat/history/{sessionId}
     * 响应: { code: 200, data: { messages: [...] } }
     */
    @GET("ai/chat/history/{sessionId}")
    suspend fun getChatHistory(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String
    ): Response<ApiResponse<ChatHistoryResponse>>

    /**
     * 获取用户的会话列表
     * GET /api/ai/chat/sessions
     * userId 从 token 中自动获取
     * 响应: { code: 200, data: [{ id, title, lastMessage, timestamp }] }
     */
    @GET("ai/chat/sessions")
    suspend fun getChatSessions(
        @Header("Authorization") token: String
    ): Response<ApiResponse<List<ChatSession>>>

    /**
     * 删除会话
     * DELETE /api/ai/chat/sessions/{sessionId}
     */
    @DELETE("ai/chat/sessions/{sessionId}")
    suspend fun deleteSession(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String
    ): Response<ApiResponse<Unit>>

    /**
     * 更新会话标题
     * PUT /api/ai/chat/sessions/{sessionId}/title
     */
    @PUT("ai/chat/sessions/{sessionId}/title")
    suspend fun updateSessionTitle(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String,
        @Body request: Map<String, String>
    ): Response<ApiResponse<ChatSessionResponse>>
}
