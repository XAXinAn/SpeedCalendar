package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.AddScheduleRequest
import com.example.speedcalendar.data.model.ApiResponse
import com.example.speedcalendar.features.home.Schedule
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ScheduleApiService {

    @GET("api/schedules/user/{userId}")
    suspend fun getSchedules(
        @Path("userId") userId: String,
        @Query("month") month: String
    ): Response<ApiResponse<List<Schedule>>>

    @POST("api/schedules")
    suspend fun addSchedule(@Body request: AddScheduleRequest): Response<ApiResponse<Schedule>>
}
