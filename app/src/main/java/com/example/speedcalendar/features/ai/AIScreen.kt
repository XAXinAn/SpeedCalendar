package com.example.speedcalendar.features.ai

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedcalendar.data.local.UserPreferences
import com.example.speedcalendar.ui.theme.Background

data class AITool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val iconBackgroundColor: Color,
    val isAvailable: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreen(
    onNavigateToChat: (String?) -> Unit = {},
    onRequestScreenCapture: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences.getInstance(context) }
    val isLoggedIn = remember { userPreferences.isLoggedIn() }

    val aiTools = listOf(
        AITool(
            id = "chat",
            name = "AI聊天",
            description = "与智能助手对话",
            icon = Icons.Default.AutoAwesome,
            iconBackgroundColor = Color(0xFF5AD8A6),
            isAvailable = true
        ),
        AITool(
            id = "floating_window",
            name = "悬浮窗",
            description = "开启一个可拖动的悬浮窗",
            icon = Icons.Default.Lightbulb,
            iconBackgroundColor = Color(0xFF7265E3),
            isAvailable = true
        ),
        AITool(
            id = "tool3",
            name = "待开发",
            description = "未来更精彩",
            icon = Icons.Default.Psychology,
            iconBackgroundColor = Color(0xFFFF6B6B),
            isAvailable = false
        ),
        AITool(
            id = "tool4",
            name = "待开发",
            description = "敬请期待",
            icon = Icons.Default.Translate,
            iconBackgroundColor = Color(0xFFFFC069),
            isAvailable = false
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI助手", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = Background
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 16.dp,
                bottom = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(aiTools) { tool ->
                AIToolCard(
                    tool = tool,
                    onClick = {
                        if (tool.isAvailable) {
                            when (tool.id) {
                                "chat" -> onNavigateToChat(null)
                                "floating_window" -> {
                                    // 检查是否已登录
                                    if (!isLoggedIn) {
                                        Toast.makeText(context, "请先登录后再使用悬浮窗功能", Toast.LENGTH_SHORT).show()
                                        return@AIToolCard
                                    }
                                    
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        // 请求截屏权限并启动悬浮窗
                                        onRequestScreenCapture()
                                    }
                                }
                                else -> { /* Do nothing */ }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AIToolCard(
    tool: AITool,
    onClick: () -> Unit
) {
    val contentColor = if (tool.isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
        enabled = tool.isAvailable
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tool.iconBackgroundColor.copy(alpha = 0.1f))
                        .padding(8.dp),
                    tint = tool.iconBackgroundColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = tool.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = contentColor
                )
            }
            Text(
                text = tool.description,
                fontSize = 12.sp,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}
