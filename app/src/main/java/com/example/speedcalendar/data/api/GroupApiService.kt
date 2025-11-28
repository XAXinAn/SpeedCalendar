package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.CreateGroupRequest
import com.example.speedcalendar.data.model.Group
import com.example.speedcalendar.data.model.GroupDetails
import com.example.speedcalendar.data.model.GroupMembership
import com.example.speedcalendar.data.model.JoinGroupRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GroupApiService {

    @POST("groups")
    suspend fun createGroup(@Body createGroupRequest: CreateGroupRequest): Group

    @POST("groups/join-with-code")
    suspend fun joinGroup(@Body joinGroupRequest: JoinGroupRequest): Group

    @GET("groups/my")
    suspend fun getMyGroups(): List<GroupMembership>

    @GET("groups/{groupId}")
    suspend fun getGroupDetails(@Path("groupId") groupId: String): GroupDetails

    @DELETE("groups/{groupId}")
    suspend fun deleteGroup(@Path("groupId") groupId: String)
}