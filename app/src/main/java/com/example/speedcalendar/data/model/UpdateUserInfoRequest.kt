package com.example.speedcalendar.data.model

/**
 * 更新用户信息请求
 */
data class UpdateUserInfoRequest(
    /**
     * 用户昵称
     */
    val username: String? = null,

    /**
     * 用户头像URL
     */
    val avatar: String? = null
)
