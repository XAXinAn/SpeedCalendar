package com.example.speedcalendar.features.ai.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speedcalendar.viewmodel.AIChatViewModel
import kotlinx.coroutines.launch

/**
 * 消息类型
 */
enum class MessageRole {
    USER, AI
}

/**
 * 消息数据类
 */
data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 聊天会话数据类
 */
data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AI聊天主界面
 * 仿照Google Gemini设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    initialMessage: String? = null,
    onBack: () -> Unit = {},
    viewModel: AIChatViewModel = viewModel()
) {
    // TODO: 从AuthViewModel获取真实用户ID
    val userId = "demo-user-id" // 临时使用固定ID，后续需要从登录状态获取

    val messages by viewModel.messages.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showSidebar by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 加载用户会话列表
    LaunchedEffect(Unit) {
        viewModel.loadUserSessions(userId)
    }

    // 处理初始消息
    LaunchedEffect(initialMessage) {
        if (!initialMessage.isNullOrEmpty() && messages.isEmpty()) {
            viewModel.sendMessage(initialMessage, userId) {
                coroutineScope.launch {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
            }
        }
    }

    // 显示错误提示
    error?.let { errorMsg ->
        LaunchedEffect(errorMsg) {
            // TODO: 显示Toast或Snackbar
            viewModel.clearError()
        }
    }

    // 计算偏移量（使用弹簧动画）
    val offsetX by animateDpAsState(
        targetValue = if (showSidebar) 280.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.8f,  // 增加阻尼，减少弹性
            stiffness = Spring.StiffnessLow
        ),
        label = "offset_animation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 主聊天区域（会随侧边栏平移）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToPx(), 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // 顶部栏
                TopBar(
                    showSidebar = showSidebar,
                    onMenuClick = {
                        if (!showSidebar) {
                            // 打开侧边栏时刷新会话列表
                            viewModel.loadUserSessions(userId)
                        }
                        showSidebar = !showSidebar
                    },
                    onBack = onBack
                )

                // 聊天内容区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        // 初始状态 - 居中显示欢迎界面
                        InitialChatView(
                            onSendMessage = { message ->
                                viewModel.sendMessage(message, userId) {
                                    coroutineScope.launch {
                                        if (messages.isNotEmpty()) {
                                            listState.animateScrollToItem(messages.size - 1)
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        // 聊天状态 - 显示消息列表
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(messages) { message ->
                                MessageBubble(message = message)
                            }
                        }
                    }
                }

                // 底部输入框（聊天状态下显示）
                if (messages.isNotEmpty()) {
                    ChatInputBar(
                        value = inputText,
                        onValueChange = { inputText = it },
                        onSend = {
                            if (inputText.isNotBlank()) {
                                val messageToSend = inputText
                                inputText = ""
                                viewModel.sendMessage(messageToSend, userId) {
                                    coroutineScope.launch {
                                        if (messages.isNotEmpty()) {
                                            listState.animateScrollToItem(messages.size - 1)
                                        }
                                    }
                                }
                            }
                        },
                        isLoading = isLoading
                    )
                }
            }

            // 半透明遮罩（侧边栏打开时显示，点击关闭侧边栏）
            if (showSidebar) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            showSidebar = false
                        }
                )
            }
        }

        // 侧边栏（覆盖在主内容上方）
        AnimatedVisibility(
            visible = showSidebar,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            ChatSidebar(
                sessions = sessions,
                onNewChat = {
                    viewModel.resetSession()
                    showSidebar = false
                },
                onSessionClick = { session ->
                    viewModel.loadChatHistory(session.id)
                    showSidebar = false
                },
                modifier = Modifier.width(280.dp)
            )
        }
    }
}

/**
 * 顶部导航栏
 */
@Composable
private fun TopBar(
    showSidebar: Boolean,
    onMenuClick: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        shadowElevation = 2.dp,
        color = Color.White,
        modifier = Modifier.statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 菜单按钮
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "菜单",
                    tint = Color(0xFF333333)
                )
            }

            Text(
                text = "小电",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )

            // 返回按钮
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color(0xFF333333)
                )
            }
        }
    }
}

/**
 * 侧边栏
 */
@Composable
private fun ChatSidebar(
    sessions: List<ChatSession>,
    onNewChat: () -> Unit,
    onSessionClick: (ChatSession) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color(0xFFF7F8FA),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 顶部搜索区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "小电",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                IconButton(onClick = { /* TODO: 搜索功能 */ }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 发起新对话按钮
            Button(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5B8FF9),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("发起新对话")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 近期对话标题
            Text(
                text = "近期对话",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )

            // 对话列表
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sessions) { session ->
                    SessionItem(
                        session = session,
                        onClick = { onSessionClick(session) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 底部设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { /* TODO: 设置 */ }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "设置和帮助",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

/**
 * 会话列表项
 */
@Composable
private fun SessionItem(
    session: ChatSession,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = session.title,
                fontSize = 14.sp,
                color = Color(0xFF333333),
                maxLines = 1
            )
        }
    }
}

/**
 * 初始聊天视图（居中显示）
 */
@Composable
private fun InitialChatView(
    onSendMessage: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 欢迎文字
        Text(
            text = "你好！",
            fontSize = 36.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 输入框
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFF7F8FA),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 附加按钮
                IconButton(
                    onClick = { /* TODO: 附加文件 */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "附加",
                        tint = Color(0xFF666666)
                    )
                }

                // 输入框
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFF333333)
                    )
                ) { innerTextField ->
                    Box {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "问问 小电",
                                color = Color(0xFF999999),
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }

                // 发送按钮
                if (inputText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onSendMessage(inputText)
                            inputText = ""
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "发送",
                            tint = Color(0xFF5B8FF9)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 消息气泡
 */
@Composable
private fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.role == MessageRole.USER) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        if (message.role == MessageRole.AI) {
            // AI头像
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5B8FF9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (message.role == MessageRole.USER) {
                Color(0xFF5B8FF9)
            } else {
                Color(0xFFF7F8FA)
            },
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                color = if (message.role == MessageRole.USER) {
                    Color.White
                } else {
                    Color(0xFF333333)
                },
                fontSize = 15.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        if (message.role == MessageRole.USER) {
            Spacer(modifier = Modifier.width(8.dp))
            // 用户头像
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9270CA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 聊天输入框（底部固定）
 */
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean = false
) {
    Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFF7F8FA)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF333333)
                        )
                    ) { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = "输入消息...",
                                    color = Color(0xFF999999),
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 发送按钮
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (value.isNotBlank() && !isLoading) Color(0xFF5B8FF9) else Color(0xFFE0E0E0)
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "发送",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

