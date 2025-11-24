package com.example.speedcalendar.features.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FilterVintage
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedcalendar.ui.theme.Background

/**
 * AI工具数据类
 */
data class AITool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val iconBackgroundColor: Color,
    val isAvailable: Boolean = true
)

/**
 * AI工具主界面
 * 仿钉钉设计，展示各种AI工具
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreen(
    onNavigateToChat: (String?) -> Unit = {}
) {
    // 定义AI工具列表
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
            id = "ocr",
            name = "OCR识别",
            description = "从图片中提取文字",
            icon = Icons.Default.DocumentScanner,
            iconBackgroundColor = Color(0xFF5B8FF9),
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
                )
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
                                "ocr" -> { /* TODO: Navigate to OCR */ }
                                else -> { /* Do nothing */ }
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * AI工具卡片
 */
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
