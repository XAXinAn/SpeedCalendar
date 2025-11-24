package com.example.speedcalendar.features.home

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedcalendar.ui.theme.PrimaryBlue
import kotlin.math.roundToInt

@Composable
fun <T> WheelPicker(
    items: List<T>,
    onItemSelected: (index: Int, item: T) -> Unit,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    itemHeight: Dp = 48.dp,
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

    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val finalIndex = centralItemIndex.coerceIn(0, items.size - 1)
            onItemSelected(finalIndex, items[finalIndex])
            state.animateScrollToItem(finalIndex)
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
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index].toString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = if (isCentered) 22.sp else 18.sp,
                            fontWeight = if (isCentered) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isCentered) PrimaryBlue else Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            val scale = if (isCentered) 1.0f else 0.8f
                            scaleX = scale
                            scaleY = scale
                            alpha = if (isCentered) 1.0f else 0.5f
                        }
                    )
                }
            }
        }
    }
}
