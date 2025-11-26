package com.example.speedcalendar.data.model

data class Schedule(
    val scheduleId: String,
    val userId: String,
    val title: String,
    val scheduleDate: String,
    val startTime: String?,
    val endTime: String?,
    val location: String?,
    val isAllDay: Boolean,
    val createdAt: Long
)
