package com.example.speedcalendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.local.UserPreferences
import com.example.speedcalendar.data.model.AddScheduleRequest
import com.example.speedcalendar.data.model.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleApiService = RetrofitClient.scheduleApiService
    private val userPreferences = UserPreferences.getInstance(application)

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSchedules(yearMonth: YearMonth) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = userPreferences.getAccessToken() // aiexpert: get token
                if (token != null) {
                    _schedules.value = scheduleApiService.getSchedules("Bearer $token", yearMonth.year, yearMonth.monthValue)
                } else {
                    _error.value = "用户未登录"
                }
            } catch (e: Exception) {
                _error.value = "无法加载日程: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSchedule(title: String, date: LocalDate, time: String?, location: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = userPreferences.getAccessToken()
                if (token != null) {
                    val newSchedule = scheduleApiService.addSchedule(
                        "Bearer $token",
                        AddScheduleRequest(
                            title = title,
                            scheduleDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            startTime = time,
                            endTime = null, // TODO: Add end time support
                            location = location,
                            isAllDay = time == null
                        )
                    )
                    _schedules.value = _schedules.value + newSchedule
                    onSuccess()
                } else {
                    _error.value = "用户未登录"
                }
            } catch (e: Exception) {
                _error.value = "添加日程失败: ${e.message}"
            }
        }
    }

    fun updateSchedule(schedule: Schedule, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = userPreferences.getAccessToken()
                if (token != null) {
                    val updatedSchedule = scheduleApiService.updateSchedule(
                        "Bearer $token",
                        schedule.scheduleId,
                        AddScheduleRequest(
                            title = schedule.title,
                            scheduleDate = schedule.scheduleDate,
                            startTime = schedule.startTime,
                            endTime = schedule.endTime,
                            location = schedule.location,
                            isAllDay = schedule.isAllDay
                        )
                    )
                    _schedules.value = _schedules.value.map {
                        if (it.scheduleId == updatedSchedule.scheduleId) updatedSchedule else it
                    }
                    onSuccess()
                } else {
                    _error.value = "用户未登录"
                }
            } catch (e: Exception) {
                _error.value = "更新日程失败: ${e.message}"
            }
        }
    }

    fun deleteSchedule(scheduleId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = userPreferences.getAccessToken()
                if (token != null) {
                    scheduleApiService.deleteSchedule("Bearer $token", scheduleId)
                    _schedules.value = _schedules.value.filterNot { it.scheduleId == scheduleId }
                    onSuccess()
                } else {
                    _error.value = "用户未登录"
                }
            } catch (e: Exception) {
                _error.value = "删除日程失败: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
