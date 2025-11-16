package com.example.speedcalendar.data.model

/**
 * API统一响应格式
 *
 * @param T 数据类型
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)
