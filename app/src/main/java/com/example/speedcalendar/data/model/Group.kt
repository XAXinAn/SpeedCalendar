package com.example.speedcalendar.data.model

data class Group(
    val id: String,
    val name: String,
    val ownerId: String,
    val invitationCode: String?
)
