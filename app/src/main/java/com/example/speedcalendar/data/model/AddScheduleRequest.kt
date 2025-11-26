package com.example.speedcalendar.data.model

import com.google.gson.annotations.SerializedName

data class AddScheduleRequest(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("scheduleDate")
    val scheduleDate: String,
    @SerializedName("startTime")
    val startTime: String?,
    @SerializedName("endTime")
    val endTime: String?,
    @SerializedName("location")
    val location: String?,
    @SerializedName("isAllDay")
    val isAllDay: Boolean
)
