package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 隐私设置API服务
 */
interface PrivacyApiService {

    /**
     * 获取用户的隐私设置
     * GET /api/privacy/settings/{userId}
     */
    @GET("privacy/settings/{userId}")
    suspend fun getPrivacySettings(
        @Path("userId") userId: String
    ): Response<ApiResponse<List<PrivacySettingDTO>>>

    /**
     * 批量更新隐私设置
     * PUT /api/privacy/settings/{userId}
     */
    @PUT("privacy/settings/{userId}")
    suspend fun updatePrivacySettings(
        @Path("userId") userId: String,
        @Body request: UpdatePrivacySettingsRequest
    ): Response<ApiResponse<Unit>>
}
