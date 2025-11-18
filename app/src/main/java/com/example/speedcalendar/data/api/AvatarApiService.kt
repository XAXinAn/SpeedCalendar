package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 头像API服务
 */
interface AvatarApiService {

    /**
     * 上传头像
     * POST /api/avatar/upload
     *
     * TODO: 生产环境可能需要修改为直传OSS
     */
    @Multipart
    @POST("avatar/upload")
    suspend fun uploadAvatar(
        @Part("userId") userId: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<Map<String, String>>>

    /**
     * 删除头像（恢复默认头像）
     * DELETE /api/avatar/{userId}
     */
    @DELETE("avatar/{userId}")
    suspend fun deleteAvatar(
        @Path("userId") userId: String
    ): Response<ApiResponse<Unit>>
}
