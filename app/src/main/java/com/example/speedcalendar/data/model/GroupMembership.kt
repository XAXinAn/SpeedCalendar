package com.example.speedcalendar.data.model

data class GroupMembership(
    val groupId: String,
    val groupName: String,
    val role: String // "admin" or "member"
)