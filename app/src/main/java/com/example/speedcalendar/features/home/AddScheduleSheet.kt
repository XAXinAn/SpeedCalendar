package com.example.speedcalendar.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddScheduleSheet(onClose: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("10:00") } // 时间占位符

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "添加日程",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /* TODO: 保存日程逻辑 */ }) {
                Icon(Icons.Filled.Check, contentDescription = "保存")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 为输入框定义统一的、无变化的颜色
        val customTextFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outline, // 获焦时边框颜色
            unfocusedBorderColor = MaterialTheme.colorScheme.outline, // 未获焦时边框颜色
        )

        // 标题输入框
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("标题") }, // 使用 placeholder 代替 label
            leadingIcon = { Icon(Icons.Filled.Title, contentDescription = "标题") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = customTextFieldColors // 应用自定义颜色
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 地点输入框
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            placeholder = { Text("地点") }, // 使用 placeholder 代替 label
            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = "地点") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = customTextFieldColors // 应用自定义颜色
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 时间选择行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* TODO: 打开时间选择器 */ }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = "时间",
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(text = time, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
