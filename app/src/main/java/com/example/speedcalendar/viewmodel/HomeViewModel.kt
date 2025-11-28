package com.example.speedcalendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.local.UserPreferences
import com.example.speedcalendar.data.model.AddScheduleRequest
import com.example.speedcalendar.data.model.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

    fun getScheduleById(scheduleId: String): Flow<Schedule?> {
        return schedules.map { list -> list.find { it.scheduleId == scheduleId } }
    }

    fun loadSchedules(yearMonth: YearMonth) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = userPreferences.getAccessToken()
                if (token != null) {
                    val response = scheduleApiService.getSchedules("Bearer $token", yearMonth.year, yearMonth.monthValue)
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.code == 200) {
                            _schedules.value = apiResponse.data ?: emptyList()
                        } else {
                            _error.value = apiResponse?.message ?: "获取日程失败"
                        }
                    } else {
                        _error.value = "网络请求失败: ${response.code()}"
                    }
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

    fun addSchedule(title: String, date: LocalDate, time: String?, location: String?, groupId: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = userPreferences.getAccessToken()
                if (token != null) {
                    val response = scheduleApiService.addSchedule(
                        "Bearer $token",
                        AddScheduleRequest(
                            title = title,
                            scheduleDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            startTime = time,
                            endTime = null, // TODO: Add end time support
                            location = location,
                            isAllDay = time == null,
                            groupId = groupId
                        )
                    )
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.code == 200 && apiResponse.data != null) {
                            _schedules.value = _schedules.value + apiResponse.data
                            onSuccess()
                        } else {
                            _error.value = apiResponse?.message ?: "添加日程失败"
                        }
                    } else {
                        _error.value = "网络请求失败: ${response.code()}"
                    }
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
                    val response = scheduleApiService.updateSchedule(
                        "Bearer $token",
                        schedule.scheduleId,
                        AddScheduleRequest(
                            title = schedule.title,
                            scheduleDate = schedule.scheduleDate,
                            startTime = schedule.startTime,
                            endTime = schedule.endTime,
                            location = schedule.location,
                            isAllDay = schedule.isAllDay,
                            groupId = schedule.groupId
                        )
                    )
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.code == 200 && apiResponse.data != null) {
                            _schedules.value = _schedules.value.map {
                                if (it.scheduleId == apiResponse.data.scheduleId) apiResponse.data else it
                            }
                            onSuccess()
                        } else {
                            _error.value = apiResponse?.message ?: "更新日程失败"
                        }
                    } else {
                        _error.value = "网络请求失败: ${response.code()}"
                    }
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
                    val response = scheduleApiService.deleteSchedule("Bearer $token", scheduleId)
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.code == 200) {
                            _schedules.value = _schedules.value.filterNot { it.scheduleId == scheduleId }
                            onSuccess()
                        } else {
                            _error.value = apiResponse?.message ?: "删除日程失败"
                        }
                    } else {
                        _error.value = "网络请求失败: ${response.code()}"
                    }
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
