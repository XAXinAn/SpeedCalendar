package com.example.speedcalendar.features.home

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
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
    itemHeight: Dp = 36.dp
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

    LaunchedEffect(centralItemIndex) {
        val index = centralItemIndex % items.size
        onItemSelected(index, items[index])
    }

    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress && state.firstVisibleItemScrollOffset != 0) {
            state.animateScrollToItem(centralItemIndex)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        LazyColumn(
            state = state,
            modifier = Modifier.height(itemHeight * 5), // Show 5 items
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = ScrollableDefaults.flingBehavior()
        ) {
            items(count = Int.MAX_VALUE) { index ->
                val itemIndex = index % items.size
                val isCentered = (index == centralItemIndex)

                Text(
                    text = items[itemIndex].toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCentered) MaterialTheme.colorScheme.primary else Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .height(itemHeight)
                        .padding(vertical = 4.dp)
                        .graphicsLayer {
                            val scale = if (isCentered) 1.2f else 0.8f
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
        }
        HorizontalDivider(modifier = Modifier.align(Alignment.Center).padding(top = itemHeight / 2), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        HorizontalDivider(modifier = Modifier.align(Alignment.Center).padding(bottom = itemHeight / 2), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int = 10,
    initialMinute: Int = 30
) {
    val amPmItems = listOf("上午", "下午")
    val hourItems = (1..12).toList()
    val minuteItems = (0..59).map { it.toString().padStart(2, '0') }

    var selectedAmPm by remember { mutableStateOf(if (initialHour < 12) "上午" else "下午") }
    var selectedHour by remember { mutableIntStateOf(if (initialHour == 0 || initialHour == 12) 12 else initialHour % 12) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("选择时间", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    WheelPicker(
                        items = amPmItems,
                        onItemSelected = { _, item -> selectedAmPm = item },
                        initialIndex = amPmItems.indexOf(selectedAmPm),
                        modifier = Modifier.weight(1f)
                    )
                    WheelPicker(
                        items = hourItems,
                        onItemSelected = { _, item -> selectedHour = item },
                        initialIndex = hourItems.indexOf(selectedHour),
                        modifier = Modifier.weight(1f)
                    )
                    WheelPicker(
                        items = minuteItems,
                        onItemSelected = { _, item -> selectedMinute = item.toInt() },
                        initialIndex = minuteItems.indexOf(selectedMinute.toString().padStart(2, '0')),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = { 
                        val finalHour = when {
                            selectedAmPm == "下午" && selectedHour != 12 -> selectedHour + 12
                            selectedAmPm == "上午" && selectedHour == 12 -> 0 // 12 AM is 00:00
                            else -> selectedHour
                        }
                        onConfirm(finalHour, selectedMinute)
                     }) { Text("确定") }
                }
            }
        }
    }
}