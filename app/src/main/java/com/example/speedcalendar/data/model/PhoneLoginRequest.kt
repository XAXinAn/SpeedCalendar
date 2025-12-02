package com.example.speedcalendar.data.model

/**
 * 手机号登录请求
 */
data class PhoneLoginRequest(
    val phone: String,
    val code: String
)

/**
 * 账号密码登录请求
 */
data class PasswordLoginRequest(
    val phone: String,
    val password: String
)

/**
 * 注册请求
 */
data class RegisterRequest(
    val phone: String,
    val password: String
)
