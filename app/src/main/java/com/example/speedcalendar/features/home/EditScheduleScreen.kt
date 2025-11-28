package com.example.speedcalendar.features.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speedcalendar.ui.theme.Background
import com.example.speedcalendar.ui.theme.PrimaryBlue
import com.example.speedcalendar.viewmodel.GroupViewModel
import com.example.speedcalendar.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(
    scheduleId: String,
    homeViewModel: HomeViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val schedule by homeViewModel.getScheduleById(scheduleId).collectAsState(initial = null)
    val adminGroups by groupViewModel.adminGroups.collectAsState()

    schedule?.let { scheduleData ->
        var title by remember { mutableStateOf(scheduleData.title) }
        var location by remember { mutableStateOf(scheduleData.location ?: "") }
        var time by remember { mutableStateOf(scheduleData.startTime) }
        var selectedGroupId by remember(scheduleData) { mutableStateOf(scheduleData.groupId) }
        var showTimePicker by remember { mutableStateOf(false) }
        var showGroupPicker by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        if (showTimePicker) {
            val (initialHour, initialMinute) = remember(time) {
                val currentTime = time
                if (currentTime != null) {
                    currentTime.split(':').map { it.toInt() }
                } else {
                    listOf(9, 0)
                }
            }
            TimePickerDialog(
                onDismiss = { showTimePicker = false },
                onConfirm = { hour, minute ->
                    time = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                    showTimePicker = false
                },
                initialHour = initialHour,
                initialMinute = initialMinute
            )
        }

        if (showGroupPicker) {
            GroupPickerDialog(
                groups = adminGroups,
                initialSelectedGroupId = selectedGroupId,
                onDismiss = { showGroupPicker = false },
                onConfirm = { 
                    selectedGroupId = it
                    showGroupPicker = false 
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除日程") },
                text = { Text("您确定要删除这个日程吗？此操作无法撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            homeViewModel.deleteSchedule(scheduleData.scheduleId) {
                                showDeleteDialog = false
                                onNavigateBack() // Close the edit screen after deletion
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("修改日程", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    windowInsets = WindowInsets(0.dp)
                )
            },
            bottomBar = {
                Button(
                    onClick = {
                        val updatedSchedule = scheduleData.copy(
                            title = title,
                            startTime = time,
                            location = location,
                            groupId = selectedGroupId
                        )
                        homeViewModel.updateSchedule(updatedSchedule) {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(16.dp),
                    enabled = title.isNotBlank()
                ) {
                    Text("保存修改", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                BorderlessInputField(icon = Icons.Default.Notes, placeholder = "标题", value = title, onValueChange = { title = it })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                BorderlessClickableField(icon = Icons.Default.Schedule, placeholder = "时间", value = time ?: "", onClick = { showTimePicker = true })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                BorderlessInputField(icon = Icons.Default.LocationOn, placeholder = "地点", value = location, onValueChange = { location = it })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                BorderlessClickableField(
                    icon = Icons.Default.Groups,
                    placeholder = "归属",
                    value = adminGroups.find { it.groupId == selectedGroupId }?.groupName ?: "个人",
                    onClick = { showGroupPicker = true }
                )
            }
        }
    }
}