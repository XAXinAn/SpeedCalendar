package com.example.speedcalendar.features.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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

/**
 * AI工具数据类
 */
data class AITool(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val isAvailable: Boolean = true
)

/**
 * AI工具主界面
 * 仿钉钉设计，展示各种AI工具
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreen() {
    // 定义AI工具列表
    val aiTools = listOf(
        AITool(
            id = "ocr",
            name = "OCR识别",
            icon = Icons.Default.DocumentScanner,
            backgroundColor = Color(0xFF5B8FF9),
            isAvailable = true
        ),
        AITool(
            id = "tool2",
            name = "待开发",
            icon = Icons.Default.AutoAwesome,
            backgroundColor = Color(0xFF5AD8A6),
            isAvailable = false
        ),
        AITool(
            id = "tool3",
            name = "待开发",
            icon = Icons.Default.Psychology,
            backgroundColor = Color(0xFFFF6B6B),
            isAvailable = false
        ),
        AITool(
            id = "tool4",
            name = "待开发",
            icon = Icons.Default.Translate,
            backgroundColor = Color(0xFFFFC069),
            isAvailable = false
        ),
        AITool(
            id = "tool5",
            name = "待开发",
            icon = Icons.Default.FilterVintage,
            backgroundColor = Color(0xFF9270CA),
            isAvailable = false
        ),
        AITool(
            id = "tool6",
            name = "待开发",
            icon = Icons.Default.Lightbulb,
            backgroundColor = Color(0xFF269A99),
            isAvailable = false
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI助手",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        // 工具网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 16.dp,
                bottom = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(aiTools) { tool ->
                AIToolCard(
                    tool = tool,
                    onClick = {
                        // TODO: 处理工具点击事件
                        if (tool.isAvailable) {
                            // 跳转到对应的工具页面
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = tool.isAvailable, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 图标容器
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (tool.isAvailable) tool.backgroundColor
                    else Color(0xFFD9D9D9)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.name,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )

            // 待开发标记
            if (!tool.isAvailable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "敬请\n期待",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 工具名称
        Text(
            text = tool.name,
            fontSize = 14.sp,
            color = if (tool.isAvailable) Color(0xFF333333) else Color(0xFF999999),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
