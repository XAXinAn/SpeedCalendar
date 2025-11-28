package com.example.speedcalendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.model.CreateGroupRequest
import com.example.speedcalendar.data.model.Group
import com.example.speedcalendar.data.model.GroupMember
import com.example.speedcalendar.data.model.GroupMembership
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {

    private val groupApiService = RetrofitClient.groupApiService

    private val _createGroupResult = MutableStateFlow<Result<Group>?>(null)
    val createGroupResult: StateFlow<Result<Group>?> = _createGroupResult

    private val _joinGroupResult = MutableStateFlow<Result<Group>?>(null)
    val joinGroupResult: StateFlow<Result<Group>?> = _joinGroupResult

    private val _myGroups = MutableStateFlow<List<GroupMembership>>(emptyList())
    val myGroups: StateFlow<List<GroupMembership>> = _myGroups

    private val _groupMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    val groupMembers: StateFlow<List<GroupMember>> = _groupMembers

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

    fun joinGroup(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val group = groupApiService.joinGroup(groupId)
                _joinGroupResult.value = Result.success(group)
            } catch (e: Exception) {
                _joinGroupResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMyGroups() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _myGroups.value = groupApiService.getMyGroups()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _groupMembers.value = groupApiService.getGroupMembers(groupId)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteGroup(groupId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                groupApiService.deleteGroup(groupId)
                onSuccess()
                loadMyGroups() // Refresh the list after deletion
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}