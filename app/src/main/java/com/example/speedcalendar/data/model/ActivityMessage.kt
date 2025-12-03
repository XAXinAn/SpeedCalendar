package com.example.speedcalendar.data.model

/**
 * 活动消息
 */
data class ActivityMessage(
    val id: String,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val tag: String?,
    val linkType: String,  // none, internal, webview, browser
    val linkUrl: String?,
    val isRead: Boolean,
    val createdAt: String  // ISO 8601 格式，如 "2024-12-03T19:55:00"
)

/**
 * 活动消息列表响应
 */
data class ActivityMessagesResponse(
    val unreadCount: Int,
    val messages: List<ActivityMessage>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)

/**
 * 未读数量响应
 */
data class UnreadCountResponse(
    val unreadCount: Int
)

/**
 * 标记已读响应
 */
data class ReadAllResponse(
    val readCount: Int
)
