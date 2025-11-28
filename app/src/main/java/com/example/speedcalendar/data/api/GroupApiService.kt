package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.CreateGroupRequest
import com.example.speedcalendar.data.model.Group
import retrofit2.http.Body
import retrofit2.http.POST

interface GroupApiService {

    @POST("groups")
    suspend fun createGroup(@Body createGroupRequest: CreateGroupRequest): Group
}