package com.example.speedcalendar.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showYearMonthPicker by remember { mutableStateOf(false) }
    var showAddScheduleSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showYearMonthPicker) {
            YearMonthPickerDialog(
                onDismiss = { showYearMonthPicker = false },
                onConfirm = { year, month ->
                    currentMonth = YearMonth.of(year, month)
                    showYearMonthPicker = false
                },
                initialYearMonth = currentMonth
            )
        }

        Scaffold(
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimatedVisibility(
                        visible = currentMonth != YearMonth.now() || selectedDate != LocalDate.now(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        FloatingActionButton(
                            onClick = {
                                currentMonth = YearMonth.now()
                                selectedDate = LocalDate.now()
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "回到今天")
                        }
                    }
                    FloatingActionButton(
                        onClick = { showAddScheduleSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End
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

                CalendarHeader(
                    yearMonth = currentMonth,
                    onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                    onYearMonthClick = { showYearMonthPicker = true }
                )
                Spacer(modifier = Modifier.height(16.dp))

                var dragAmount by remember { mutableStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragAmount = 0f },
                                onHorizontalDrag = { _, horizontalDragAmount ->
                                    dragAmount += horizontalDragAmount
                                },
                                onDragEnd = {
                                    val threshold = screenWidthPx / 5
                                    if (dragAmount < -threshold) { // Swiped left
                                        currentMonth = currentMonth.plusMonths(1)
                                    } else if (dragAmount > threshold) { // Swiped right
                                        currentMonth = currentMonth.minusMonths(1)
                                    }
                                }
                            )
                        }
                ) {
                    AnimatedContent(
                        targetState = currentMonth,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                            } else {
                                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                            }
                        },
                        label = "CalendarAnimation"
                    ) { month ->
                        CalendarGrid(
                            yearMonth = month,
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showAddScheduleSheet,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            AddScheduleSheet(onClose = { showAddScheduleSheet = false })
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
                .clickable(remember { MutableInteractionSource() }, null, onClick = onYearMonthClick)
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
    val firstDayOfMonth = yearMonth.atDay(1)
    val paddingDays = (firstDayOfMonth.dayOfWeek.value - 1) // Monday is 1, Sunday is 7

    val startDate = firstDayOfMonth.minusDays(paddingDays.toLong())
    val numRows = 6 // Always display up to 6 rows

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        repeat(numRows) { weekIndex ->
            val weekDates = (0..6).map {
                startDate.plusDays((weekIndex * 7 + it).toLong())
            }
            val showRow = weekDates.any { YearMonth.from(it) == yearMonth }

            if (showRow) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDates.forEach { date ->
                        val isCurrentMonth = YearMonth.from(date) == yearMonth

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            val isSelected = selectedDate == date
                            val isToday = date == LocalDate.now()

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color = if (isSelected && isCurrentMonth) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable(
                                        enabled = isCurrentMonth,
                                        onClick = { onDateSelected(date) },
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    color = when {
                                        isSelected && isCurrentMonth -> MaterialTheme.colorScheme.onPrimary
                                        isToday && isCurrentMonth -> MaterialTheme.colorScheme.primary
                                        isCurrentMonth -> MaterialTheme.colorScheme.onBackground
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
