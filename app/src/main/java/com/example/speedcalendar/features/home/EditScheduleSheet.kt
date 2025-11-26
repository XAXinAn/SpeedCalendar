package com.example.speedcalendar.features.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedcalendar.data.model.Schedule
import com.example.speedcalendar.ui.theme.Background
import com.example.speedcalendar.ui.theme.PrimaryBlue
import com.example.speedcalendar.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleSheet(schedule: Schedule, homeViewModel: HomeViewModel, onClose: () -> Unit) {
    var title by remember { mutableStateOf(schedule.title) }
    var location by remember { mutableStateOf(schedule.location ?: "") }
    var time by remember { mutableStateOf(schedule.startTime) }
    var showTimePicker by remember { mutableStateOf(false) }
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除日程") },
            text = { Text("您确定要删除这个日程吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        homeViewModel.deleteSchedule(schedule.scheduleId) {
                            showDeleteDialog = false
                            onClose() // Close the edit sheet after deletion
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
                    IconButton(onClick = onClose) {
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
                )
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val updatedSchedule = schedule.copy(
                        title = title,
                        startTime = time,
                        location = location
                    )
                    homeViewModel.updateSchedule(updatedSchedule) {
                        onClose()
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
        }
    }
}
