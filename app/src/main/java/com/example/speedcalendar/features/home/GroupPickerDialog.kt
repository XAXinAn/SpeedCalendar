package com.example.speedcalendar.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.speedcalendar.data.model.GroupMembership
import com.example.speedcalendar.ui.theme.Background

@Composable
fun GroupPickerDialog(
    groups: List<GroupMembership>,
    initialSelectedGroupId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    val options = listOf(null to "个人") + groups.map { it.groupId to it.groupName }
    val initialIndex = options.indexOfFirst { it.first == initialSelectedGroupId }.coerceAtLeast(0)

    var selectedIndex by remember { mutableStateOf(initialIndex) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("选择归属", style = MaterialTheme.typography.titleLarge)
                WheelPicker(
                    items = options.map { it.second },
                    initialIndex = initialIndex,
                    onItemSelected = { index, _ -> selectedIndex = index }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    TextButton(onClick = {
                        onConfirm(options[selectedIndex].first)
                    }) {
                        Text("确认")
                    }
                }
            }
        }
    }
}
