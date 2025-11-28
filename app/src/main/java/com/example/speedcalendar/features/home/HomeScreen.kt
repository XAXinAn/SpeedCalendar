package com.example.speedcalendar.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speedcalendar.data.model.Schedule
import com.example.speedcalendar.ui.theme.Background
import com.example.speedcalendar.ui.theme.PrimaryBlue
import com.example.speedcalendar.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(homeViewModel: HomeViewModel = viewModel()) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showYearMonthPicker by remember { mutableStateOf(false) }
    var showAddScheduleSheet by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }

    val schedules by homeViewModel.schedules.collectAsState()

    LaunchedEffect(currentMonth) {
        homeViewModel.loadSchedules(currentMonth)
    }

    val selectedDateSchedules = remember(selectedDate, schedules) {
        schedules.filter { LocalDate.parse(it.scheduleDate) == selectedDate }
    }

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
            containerColor = Background,
            topBar = {
                CalendarTopAppBar(
                    yearMonth = currentMonth,
                    onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                    onTodayClick = {
                        currentMonth = YearMonth.now()
                        selectedDate = LocalDate.now()
                    },
                    onYearMonthClick = { showYearMonthPicker = true }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddScheduleSheet = true },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp, 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
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
                                    if (dragAmount < -threshold) {
                                        currentMonth = currentMonth.plusMonths(1)
                                    } else if (dragAmount > threshold) {
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
                            schedules = schedules,
                            onDateSelected = { selectedDate = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedDateSchedules.isNotEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectedDateSchedules) { schedule ->
                            ScheduleItem(schedule = schedule) {
                                editingSchedule = schedule
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("今天没有日程安排", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showAddScheduleSheet,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            AddScheduleSheet(
                homeViewModel = homeViewModel,
                selectedDate = selectedDate,
                onClose = { showAddScheduleSheet = false }
            )
        }

        AnimatedVisibility(
            visible = editingSchedule != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            editingSchedule?.let {
                EditScheduleSheet(
                    schedule = it,
                    homeViewModel = homeViewModel,
                    onClose = { editingSchedule = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTopAppBar(
    yearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onYearMonthClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "${yearMonth.year}年 ${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.CHINESE)}",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onYearMonthClick)
            )
        },
        navigationIcon = {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月", tint = PrimaryBlue)
            }
        },
        actions = {
            IconButton(onClick = onTodayClick) {
                Icon(Icons.Default.Today, contentDescription = "回到今天", tint = PrimaryBlue)
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月", tint = PrimaryBlue)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Background
        ),
        windowInsets = WindowInsets(0.dp)
    )
}

@Composable
fun CalendarGrid(
    modifier: Modifier = Modifier,
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    schedules: List<Schedule>,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val paddingDays = (firstDayOfMonth.dayOfWeek.value - 1)

    val startDate = firstDayOfMonth.minusDays(paddingDays.toLong())
    val numRows = 6

    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        repeat(numRows) { weekIndex ->
            val weekDates = (0..6).map {
                startDate.plusDays((weekIndex * 7 + it).toLong())
            }
            val showRow = weekDates.any { YearMonth.from(it) == yearMonth }

            if (showRow) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    weekDates.forEach { date ->
                        val isCurrentMonth = YearMonth.from(date) == yearMonth
                        val hasSchedule = schedules.any { LocalDate.parse(it.scheduleDate) == date } && isCurrentMonth

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
                                    .clip(CircleShape)
                                    .background(color = if (isSelected && isCurrentMonth) PrimaryBlue else Color.Transparent)
                                    .clickable(
                                        enabled = isCurrentMonth,
                                        onClick = { onDateSelected(date) },
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        fontWeight = if (isSelected && isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected && isCurrentMonth -> Color.White
                                            isToday && isCurrentMonth -> PrimaryBlue
                                            isCurrentMonth -> MaterialTheme.colorScheme.onBackground
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        }
                                    )
                                    if (hasSchedule) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(
                                                    color = if (isSelected && isCurrentMonth) Color.White else PrimaryBlue,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                 }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ScheduleItem(schedule: Schedule, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = schedule.startTime ?: "全天",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (schedule.location != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = schedule.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}