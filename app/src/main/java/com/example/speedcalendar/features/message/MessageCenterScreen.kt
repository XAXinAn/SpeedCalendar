package com.example.speedcalendar.features.message

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.speedcalendar.data.model.ActivityMessage
import com.example.speedcalendar.ui.theme.Background
import com.example.speedcalendar.ui.theme.PrimaryBlue
import com.example.speedcalendar.viewmodel.MessageCenterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageCenterScreen(
    onBack: () -> Unit,
    onNavigateToWebView: (String) -> Unit,
    viewModel: MessageCenterViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // 初次加载
    LaunchedEffect(Unit) {
        viewModel.loadMessages(refresh = true)
    }

    // 显示错误
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 滚动到底部时加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= messages.size - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore }
            .collect { shouldLoad ->
                if (shouldLoad && !isLoading && messages.isNotEmpty()) {
                    viewModel.loadMoreMessages()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消息中心", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("全部已读", color = PrimaryBlue, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadMessages(refresh = true) },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (messages.isEmpty() && !isLoading && !isRefreshing) {
                EmptyMessageView()
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageCard(
                            message = message,
                            onClick = {
                                // 标记已读
                                if (!message.isRead) {
                                    viewModel.markAsRead(message.id)
                                }
                                // 处理跳转
                                message.linkUrl?.let { url ->
                                    when (message.linkType) {
                                        "webview" -> onNavigateToWebView(url)
                                        "browser" -> {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        )
                    }

                    // 加载更多指示器
                    if (isLoading && messages.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                }
            }

            // 初始加载指示器
            if (isLoading && messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: ActivityMessage,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isRead)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (message.isRead) 1.dp else 3.dp
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 图片（如果有）
            message.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 未读标记
                if (!message.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // 标题
                Text(
                    text = message.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 内容
            Text(
                text = message.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 底部：标签 + 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标签
                message.tag?.let { tag ->
                    TagChip(tag = tag)
                } ?: Spacer(modifier = Modifier.weight(1f))

                // 时间
                Text(
                    text = formatTime(message.createdAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun TagChip(tag: String) {
    val (backgroundColor, textColor) = when (tag) {
        "活动" -> PrimaryBlue.copy(alpha = 0.15f) to PrimaryBlue
        "公告" -> Color(0xFFFF9800).copy(alpha = 0.15f) to Color(0xFFE65100)
        "福利" -> Color(0xFF4CAF50).copy(alpha = 0.15f) to Color(0xFF2E7D32)
        "新功能" -> Color(0xFF9C27B0).copy(alpha = 0.15f) to Color(0xFF6A1B9A)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = tag,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun EmptyMessageView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📭",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无消息",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(isoTime: String): String {
    return try {
        val normalizedTime = isoTime.replace("T", " ").substringBefore(".")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(normalizedTime) ?: return isoTime
        val timestamp = date.time
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        when {
            diff < 0 -> isoTime.substring(5, 16).replace("T", " ")
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000}分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000}小时前"
            diff < 604_800_000 -> "${diff / 86_400_000}天前"
            else -> {
                val outputSdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                outputSdf.format(date)
            }
        }
    } catch (e: Exception) {
        isoTime.takeLast(10)
    }
}
