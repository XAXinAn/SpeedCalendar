package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

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
     * 账号密码登录
     * POST /api/auth/login
     */
    @POST("auth/login")
    suspend fun login(@Body request: PasswordLoginRequest): Response<ApiResponse<LoginResponse>>

    /**
     * 注册
     * POST /api/auth/register
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<LoginResponse>>

    /**
     * 更新用户信息
     * PUT /api/auth/user/{userId}
     */
    @PUT("auth/user/{userId}")
    suspend fun updateUserInfo(
        @Path("userId") userId: String,
        @Body request: UpdateUserInfoRequest
    ): Response<ApiResponse<UserInfo>>

/**
     * 健康检查
     * GET /api/auth/health
     */
    @GET("auth/health")
    suspend fun health(): Response<ApiResponse<String>>

    /**
     * 刷新Token
     * POST /api/auth/refresh
     */
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<RefreshTokenResponse>>
}