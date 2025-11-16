package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 认证API服务
 */
interface AuthApiService {

    /**
     * 发送验证码
     * POST /api/auth/code
     */
    @POST("auth/code")
    suspend fun sendCode(@Body request: SendCodeRequest): Response<ApiResponse<Unit>>

    /**
     * 手机号登录
     * POST /api/auth/login/phone
     */
    @POST("auth/login/phone")
    suspend fun phoneLogin(@Body request: PhoneLoginRequest): Response<ApiResponse<LoginResponse>>

    /**
     * 健康检查
     * GET /api/auth/health
     */
    @GET("auth/health")
    suspend fun health(): Response<ApiResponse<String>>
}
