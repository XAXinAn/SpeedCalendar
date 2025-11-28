package com.example.speedcalendar.data.model

data class AddScheduleRequest(
    val title: String,
    val scheduleDate: String,
    val startTime: String?,
    val endTime: String?,
    val location: String?,
    val isAllDay: Boolean,
    val groupId: String?
)
