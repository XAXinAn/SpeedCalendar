package com.example.speedcalendar.data.model

/**
 * 登录响应
 */
data class LoginResponse(
    val userId: String,
    val token: String,
    val refreshToken: String,
    val expiresIn: Long,
    val userInfo: UserInfo
)

/**
 * 用户信息
 */
data class UserInfo(
    val userId: String,
    val phone: String?,
    val email: String?,
    val username: String?,
    val avatar: String?,
    val gender: Int?,
    val birthday: String?,
    val bio: String?,
    val loginType: String?,
    val createdAt: String?
)
