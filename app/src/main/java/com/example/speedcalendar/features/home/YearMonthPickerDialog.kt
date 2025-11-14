package com.example.speedcalendar.features.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import java.time.YearMonth

@Composable
fun YearMonthPickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
    initialYearMonth: YearMonth = YearMonth.now()
) {
    val currentYear = YearMonth.now().year
    val years = (currentYear - 100..currentYear + 100).map { it.toString() }
    val months = (1..12).map { it.toString() }

    var selectedYear by remember { mutableStateOf(initialYearMonth.year) }
    var selectedMonth by remember { mutableStateOf(initialYearMonth.monthValue) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "选择年月",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    WheelPicker(
                        items = years,
                        onItemSelected = { _, item -> selectedYear = item.toInt() },
                        initialIndex = years.indexOf(selectedYear.toString()),
                        modifier = Modifier.weight(1f)
                    )
                    WheelPicker(
                        items = months,
                        onItemSelected = { _, item -> selectedMonth = item.toInt() },
                        initialIndex = months.indexOf(selectedMonth.toString()),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = {
                        onConfirm(selectedYear, selectedMonth)
                    }) { Text("确定") }
                }
            }
        }
    }
}
