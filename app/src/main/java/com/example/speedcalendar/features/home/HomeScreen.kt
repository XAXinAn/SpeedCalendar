package com.example.speedcalendar.features.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showYearMonthPicker by remember { mutableStateOf(false) }

    if (showYearMonthPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showYearMonthPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showYearMonthPicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newSelectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            selectedDate = newSelectedDate
                            currentMonth = YearMonth.from(newSelectedDate)
                        }
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showYearMonthPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: 处理点击事件 */ },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        },
        floatingActionButtonPosition = FabPosition.End // 设置按钮在右下角
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val screenWidthPx = with(LocalDensity.current) {
                LocalConfiguration.current.screenWidthDp.dp.toPx()
            }
            val offsetX = remember { Animatable(0f) }
            val coroutineScope = rememberCoroutineScope()

            fun animateMonthChange(isNext: Boolean) {
                coroutineScope.launch {
                    val target = if (isNext) -screenWidthPx else screenWidthPx
                    offsetX.animateTo(target, animationSpec = tween(durationMillis = 300))
                    currentMonth = if (isNext) currentMonth.plusMonths(1) else currentMonth.minusMonths(1)
                    offsetX.snapTo(0f)
                }
            }

            CalendarHeader(
                yearMonth = currentMonth,
                onPreviousMonth = { animateMonthChange(false) },
                onNextMonth = { animateMonthChange(true) },
                onYearMonthClick = { showYearMonthPicker = true }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(currentMonth) { // 当月份改变时，重启手势检测
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                            },
                            onDragEnd = {
                                coroutineScope.launch {
                                    val threshold = screenWidthPx / 4
                                    when {
                                        offsetX.value < -threshold -> animateMonthChange(true)
                                        offsetX.value > threshold -> animateMonthChange(false)
                                        else -> offsetX.animateTo(0f, animationSpec = tween(200))
                                    }
                                }
                            }
                        )
                    }
            ) {
                CalendarGrid(
                    modifier = Modifier.offset { IntOffset((-screenWidthPx + offsetX.value).roundToInt(), 0) },
                    yearMonth = currentMonth.minusMonths(1),
                    selectedDate = null,
                    onDateSelected = {}
                )
                CalendarGrid(
                    modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) },
                    yearMonth = currentMonth,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
                CalendarGrid(
                    modifier = Modifier.offset { IntOffset((screenWidthPx + offsetX.value).roundToInt(), 0) },
                    yearMonth = currentMonth.plusMonths(1),
                    selectedDate = null,
                    onDateSelected = {}
                )
            }
        }
    }
}


@Composable
fun CalendarHeader(
    yearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onYearMonthClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
        }
        Text(
            text = "${yearMonth.year}年 ${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.CHINESE)}",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onYearMonthClick
                )
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
        }
    }
}

@Composable
fun CalendarGrid(
    modifier: Modifier = Modifier,
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val paddingDays = firstDayOfMonth.dayOfWeek.value % 7
    val daysOfWeek = remember { listOf("日", "一", "二", "三", "四", "五", "六") }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = paddingDays + daysInMonth
        val numRows = (totalCells + 6) / 7
        repeat(numRows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { colIndex ->
                    val dayIndex = (it * 7 + colIndex) - paddingDays
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    ) {
                        if (dayIndex >= 0 && dayIndex < daysInMonth) {
                            val dayValue = dayIndex + 1
                            val date = yearMonth.atDay(dayValue)
                            val isSelected = selectedDate == date

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable(remember { MutableInteractionSource() }, indication = null) {
                                        onDateSelected(date)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayValue.toString(),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
