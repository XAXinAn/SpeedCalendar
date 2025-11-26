package com.example.speedcalendar.data.model

/**
 * 刷新Token请求
 */
data class RefreshTokenRequest(
    val refreshToken: String
)

/**
 * 刷新Token响应
 */
data class RefreshTokenResponse(
    val token: String,
    val refreshToken: String,
    val expiresIn: Long
)
