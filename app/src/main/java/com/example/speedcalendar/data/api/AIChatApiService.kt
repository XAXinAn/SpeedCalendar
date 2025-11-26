package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.*
import com.example.speedcalendar.viewmodel.ChatSession
import retrofit2.Response
import retrofit2.http.*

/**
 * AI聊天API服务
 */
interface AIChatApiService {

    /**
     * 发送聊天消息
     * POST /api/ai/chat/message
     */
    @POST("ai/chat/message")
    suspend fun sendMessage(@Body request: ChatMessageRequest): Response<ApiResponse<ChatMessageResponse>>

    /**
     * 创建新的聊天会话
     * POST /api/ai/chat/session
     */
    @POST("ai/chat/session")
    suspend fun createSession(@Body request: CreateSessionRequest): Response<ApiResponse<ChatSessionResponse>>

    /**
     * 获取聊天历史
     * GET /api/ai/chat/history/{sessionId}
     */
    @GET("ai/chat/history/{sessionId}")
    suspend fun getChatHistory(
        @Path("sessionId") sessionId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponse<ChatHistoryResponse>>

    /**
     * 获取用户的会话列表
     * GET /api/ai/chat/sessions/user/{userId}
     */
    @GET("ai/chat/sessions/user/{userId}")
    suspend fun getChatSessions(@Path("userId") userId: String): Response<ApiResponse<List<ChatSession>>>


    /**
     * 删除会话
     * DELETE /api/ai/chat/session/{sessionId}
     */
    @DELETE("ai/chat/session/{sessionId}")
    suspend fun deleteSession(
        @Path("sessionId") sessionId: String,
        @Query("userId") userId: String
    ): Response<ApiResponse<Unit>>

    /**
     * 更新会话标题
     * PUT /api/ai/chat/session/{sessionId}/title
     */
    @PUT("ai/chat/session/{sessionId}/title")
    suspend fun updateSessionTitle(
        @Path("sessionId") sessionId: String,
        @Body request: Map<String, String>
    ): Response<ApiResponse<ChatSessionResponse>>
}
