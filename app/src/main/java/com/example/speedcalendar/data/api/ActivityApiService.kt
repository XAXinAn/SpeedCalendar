package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.ActivityMessage
import com.example.speedcalendar.data.model.ActivityMessagesResponse
import com.example.speedcalendar.data.model.ApiResponse
import com.example.speedcalendar.data.model.ReadAllResponse
import com.example.speedcalendar.data.model.UnreadCountResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 活动消息 API 接口
 */
interface ActivityApiService {

    /**
     * 获取活动消息列表
     */
    @GET("activity/messages")
    suspend fun getMessages(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<ActivityMessagesResponse>>

    /**
     * 获取未读消息数量
     */
    @GET("activity/unread-count")
    suspend fun getUnreadCount(): Response<ApiResponse<UnreadCountResponse>>

    /**
     * 标记单条消息已读
     */
    @POST("activity/messages/{messageId}/read")
    suspend fun markAsRead(
        @Path("messageId") messageId: String
    ): Response<ApiResponse<Nothing>>

    /**
     * 标记全部已读
     */
    @POST("activity/messages/read-all")
    suspend fun markAllAsRead(): Response<ApiResponse<ReadAllResponse>>

    /**
     * 获取消息详情
     */
    @GET("activity/messages/{messageId}")
    suspend fun getMessageDetail(
        @Path("messageId") messageId: String
    ): Response<ApiResponse<ActivityMessage>>
}
