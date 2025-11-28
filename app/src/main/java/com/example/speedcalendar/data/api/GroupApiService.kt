package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.CreateGroupRequest
import com.example.speedcalendar.data.model.Group
import com.example.speedcalendar.data.model.GroupMember
import com.example.speedcalendar.data.model.GroupMembership
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GroupApiService {

    @POST("groups")
    suspend fun createGroup(@Body createGroupRequest: CreateGroupRequest): Group

    @POST("groups/{groupId}/join")
    suspend fun joinGroup(@Path("groupId") groupId: String): Group

    @GET("groups/my")
    suspend fun getMyGroups(): List<GroupMembership>

    @GET("groups/{groupId}/members")
    suspend fun getGroupMembers(@Path("groupId") groupId: String): List<GroupMember>

    @DELETE("groups/{groupId}")
    suspend fun deleteGroup(@Path("groupId") groupId: String)
}