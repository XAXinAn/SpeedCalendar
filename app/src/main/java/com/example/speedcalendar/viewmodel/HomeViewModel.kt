package com.example.speedcalendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.local.UserPreferences
import com.example.speedcalendar.data.model.AddScheduleRequest
import com.example.speedcalendar.features.home.Schedule
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
        // 暂时使用写死的数据
        val today = LocalDate.now()
        val hardcodedSchedules = listOf(
            Schedule(
                scheduleId = "1",
                userId = "demo-user",
                title = "与团队成员的技术交流会议",
                scheduleDate = today.toString(),
                startTime = "14:00",
                endTime = "15:00",
                location = "线上会议 - Google Meet",
                isAllDay = false,
                createdAt = System.currentTimeMillis()
            ),
            Schedule(
                scheduleId = "2",
                userId = "demo-user",
                title = "健身",
                scheduleDate = today.toString(),
                startTime = "18:00",
                endTime = "19:00",
                location = "健身房",
                isAllDay = false,
                createdAt = System.currentTimeMillis()
            )
        )
        _schedules.value = hardcodedSchedules
    }

    fun addSchedule(title: String, date: LocalDate, time: String?, location: String?, onSuccess: () -> Unit) {
        // TODO: 当后端准备好后，这里也需要连接到 addSchedule API
        onSuccess()
    }

    fun updateSchedule(schedule: Schedule, onSuccess: () -> Unit) {
        _schedules.value = _schedules.value.map {
            if (it.scheduleId == schedule.scheduleId) schedule else it
        }
        onSuccess()
    }

    fun deleteSchedule(scheduleId: String, onSuccess: () -> Unit) {
        _schedules.value = _schedules.value.filterNot { it.scheduleId == scheduleId }
        onSuccess()
    }

    fun clearError() {
        _error.value = null
    }
}
