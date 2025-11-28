package com.example.speedcalendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.model.CreateGroupRequest
import com.example.speedcalendar.data.model.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {

    private val groupApiService = RetrofitClient.groupApiService

    private val _createGroupResult = MutableStateFlow<Result<Group>?>(null)
    val createGroupResult: StateFlow<Result<Group>?> = _createGroupResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun createGroup(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = CreateGroupRequest(name = name)
                val group = groupApiService.createGroup(request)
                _createGroupResult.value = Result.success(group)
            } catch (e: Exception) {
                _createGroupResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}