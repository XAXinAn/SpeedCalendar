package com.example.speedcalendar.data.api

import com.example.speedcalendar.data.model.AddScheduleRequest
import com.example.speedcalendar.data.model.ApiResponse
import com.example.speedcalendar.data.model.Schedule
import retrofit2.Response
import retrofit2.http.*

interface ScheduleApiService {

    @GET("schedules")
    suspend fun getSchedules(
        @Header("Authorization") token: String,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<ApiResponse<List<Schedule>>>

    @POST("schedules")
    suspend fun addSchedule(
        @Header("Authorization") token: String,
        @Body schedule: AddScheduleRequest
    ): Response<ApiResponse<Schedule>>

    @PUT("schedules/{scheduleId}")
    suspend fun updateSchedule(
        @Header("Authorization") token: String,
        @Path("scheduleId") scheduleId: String,
        @Body schedule: AddScheduleRequest
    ): Response<ApiResponse<Schedule>>

    @DELETE("schedules/{scheduleId}")
    suspend fun deleteSchedule(
        @Header("Authorization") token: String,
        @Path("scheduleId") scheduleId: String
    ): Response<ApiResponse<Unit>>
}
