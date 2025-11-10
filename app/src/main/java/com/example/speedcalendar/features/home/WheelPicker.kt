package com.example.speedcalendar.features.home

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

@Composable
fun <T> WheelPicker(
    items: List<T>,
    onItemSelected: (index: Int, item: T) -> Unit,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    itemHeight: Dp = 36.dp,
    textVerticalOffset: Dp = 0.dp // Corrected parameter
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    val centralItemIndex by remember {
        derivedStateOf {
            val firstVisibleIndex = state.firstVisibleItemIndex
            val firstVisibleOffset = state.firstVisibleItemScrollOffset
            (firstVisibleIndex + (firstVisibleOffset / itemHeightPx).roundToInt())
        }
    }

    // **关键改动**：只在滚动停止时，才向外通知结果
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val finalIndex = centralItemIndex.coerceIn(0, items.size - 1)
            onItemSelected(finalIndex, items[finalIndex])
            state.animateScrollToItem(finalIndex) // 自动吸附到最近的选项
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        LazyColumn(
            state = state,
            modifier = Modifier.height(itemHeight * 5),
            contentPadding = PaddingValues(vertical = itemHeight * 2),
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = ScrollableDefaults.flingBehavior()
        ) {
            items(count = items.size) { index ->
                val isCentered = (index == centralItemIndex)

                Box(
                    modifier = Modifier.height(itemHeight),
                    contentAlignment = Alignment.Center // Use simple Center alignment
                ) {
                    Text(
                        text = items[index].toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCentered) MaterialTheme.colorScheme.primary else Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .offset(y = textVerticalOffset) // Apply offset to the Text directly
                            .graphicsLayer {
                                val scale = if (isCentered) 1.2f else 0.8f
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int = 10,
    initialMinute: Int = 30
) {
    val hourItems = (0..23).map { it.toString().padStart(2, '0') }
    val minuteItems = (0..59).map { it.toString().padStart(2, '0') }

    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("选择时间", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    WheelPicker(
                        items = hourItems,
                        onItemSelected = { _, item -> selectedHour = item.toInt() },
                        initialIndex = hourItems.indexOf(selectedHour.toString().padStart(2, '0')),
                        modifier = Modifier.weight(1f),
                        textVerticalOffset = 4.dp // Use the correct parameter
                    )
                    WheelPicker(
                        items = minuteItems,
                        onItemSelected = { _, item -> selectedMinute = item.toInt() },
                        initialIndex = minuteItems.indexOf(selectedMinute.toString().padStart(2, '0')),
                        modifier = Modifier.weight(1f),
                        textVerticalOffset = 4.dp // Use the correct parameter
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = { 
                        onConfirm(selectedHour, selectedMinute)
                     }) { Text("确定") }
                }
            }
        }
    }
}
