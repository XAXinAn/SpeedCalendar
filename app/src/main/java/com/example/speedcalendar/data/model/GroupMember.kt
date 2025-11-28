package com.example.speedcalendar.data.model

data class GroupMember(
    val userId: String,
    val username: String,
    val role: String // "admin" or "member"
)