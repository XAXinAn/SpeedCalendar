package com.example.speedcalendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.model.CreateGroupRequest
import com.example.speedcalendar.data.model.Group
import com.example.speedcalendar.data.model.GroupDetails
import com.example.speedcalendar.data.model.GroupMembership
import com.example.speedcalendar.data.model.JoinGroupRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {

    private val groupApiService = RetrofitClient.groupApiService

    private val _createGroupResult = MutableStateFlow<Result<Group>?>(null)
    val createGroupResult: StateFlow<Result<Group>?> = _createGroupResult

    private val _joinGroupResult = MutableStateFlow<Result<Group>?>(null)
    val joinGroupResult: StateFlow<Result<Group>?> = _joinGroupResult

    private val _myGroups = MutableStateFlow<List<GroupMembership>>(emptyList())
    val myGroups: StateFlow<List<GroupMembership>> = _myGroups.asStateFlow()

    val adminGroups: StateFlow<List<GroupMembership>> = myGroups.map { groups ->
        groups.filter { it.role == "admin" }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _groupDetails = MutableStateFlow<GroupDetails?>(null)
    val groupDetails: StateFlow<GroupDetails?> = _groupDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMyGroups()
    }

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

    fun joinGroup(invitationCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = JoinGroupRequest(invitationCode = invitationCode)
                val group = groupApiService.joinGroup(request)
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

    fun loadGroupDetails(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _groupDetails.value = groupApiService.getGroupDetails(groupId)
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

    fun clearCreateGroupResult() {
        _createGroupResult.value = null
    }
}