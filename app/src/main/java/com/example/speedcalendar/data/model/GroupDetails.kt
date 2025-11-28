package com.example.speedcalendar.data.model

data class GroupDetails(
    val id: String,
    val name: String,
    val ownerId: String,
    val currentUserRole: String,
    val invitationCode: String?,
    val members: List<GroupMember>
)
