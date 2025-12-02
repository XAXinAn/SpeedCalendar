package com.example.speedcalendar.features.ai.chat

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speedcalendar.ui.theme.Background
import com.example.speedcalendar.ui.theme.PrimaryBlue
import com.example.speedcalendar.utils.SparkChainSpeechHelper
import com.example.speedcalendar.viewmodel.AIChatViewModel
import com.example.speedcalendar.viewmodel.ChatSession
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageRole {
    USER, AI
}

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    initialMessage: String? = null,
    onBack: () -> Unit = {},
    viewModel: AIChatViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // 预加载 OCR 引擎
    LaunchedEffect(Unit) {
        viewModel.loadSessions()
        viewModel.initOcr()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawer(
                viewModel = viewModel,
                onSessionClick = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        AIChatContent(
            viewModel = viewModel,
            initialMessage = initialMessage,
            onMenuClick = {
                coroutineScope.launch { drawerState.open() }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatContent(
    viewModel: AIChatViewModel,
    initialMessage: String?,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val isOcrProcessing by viewModel.isOcrProcessing.collectAsState()
    val isOcrReady by viewModel.isOcrReady.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 快速添加模式状态
    var isQuickAddMode by remember { mutableStateOf(false) }
    
    // 底部菜单状态
    var showImagePickerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    // 相机权限相关状态
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // 语音识别相关状态
    val speechHelper = remember { SparkChainSpeechHelper(context) }
    val speechState by speechHelper.state.collectAsState()
    val speechVolume by speechHelper.volume.collectAsState()
    var isRecording by remember { mutableStateOf(false) }
    var showAudioPermissionDialog by remember { mutableStateOf(false) }
    
    // 录音权限请求
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限授予，开始语音识别
            speechHelper.startListening()
            isRecording = true
        } else {
            showAudioPermissionDialog = true
        }
    }
    
    // 检查并请求录音权限
    fun requestAudioPermissionAndStart() {
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    
    // 处理语音识别结果
    LaunchedEffect(speechState) {
        when (val state = speechState) {
            is SparkChainSpeechHelper.RecognitionState.Result -> {
                if (state.isFinal && state.text.isNotBlank()) {
                    // 识别完成，发送消息
                    isRecording = false
                    viewModel.sendMessage(state.text) {
                        coroutineScope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }
                    speechHelper.resetState()
                }
            }
            is SparkChainSpeechHelper.RecognitionState.Error -> {
                isRecording = false
                Toast.makeText(context, "识别失败: ${state.message}", Toast.LENGTH_SHORT).show()
                speechHelper.resetState()
            }
            else -> {}
        }
    }
    
    // 释放语音识别资源
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            speechHelper.destroy()
        }
    }
    
    // 拍照临时文件 Uri
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    // 创建拍照临时文件
    fun createTempPhotoUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "OCR_${timeStamp}.jpg"
        val storageDir = File(context.cacheDir, "photos")
        storageDir.mkdirs()
        val imageFile = File(storageDir, imageFileName)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }
    
    // 图片选择器（从相册）
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.processImageForOcr(it) { result ->
                inputText = result
            }
        }
    }
    
    // 拍照
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.processImageForOcr(tempPhotoUri!!) { result ->
                inputText = result
            }
        }
    }
    
    // 相机权限请求
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限授予，启动相机
            tempPhotoUri = createTempPhotoUri()
            takePictureLauncher.launch(tempPhotoUri!!)
        } else {
            // 权限被拒绝，显示解释对话框或设置引导
            showCameraPermissionDialog = true
        }
    }
    
    // 检查并请求相机权限
    fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(initialMessage) {
        if (!initialMessage.isNullOrEmpty() && messages.isEmpty()) {
            viewModel.sendMessage(initialMessage) {
                coroutineScope.launch {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
            }
        }
    }

    // 显示错误消息（如用户未登录提示）
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    // 相机权限解释对话框
    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            title = { Text("需要相机权限") },
            text = { Text("拍照识别功能需要使用相机，请授予相机权限。如果您之前拒绝了权限，可能需要在设置中手动开启。") },
            confirmButton = {
                TextButton(onClick = {
                    showCameraPermissionDialog = false
                    showSettingsDialog = true
                }) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCameraPermissionDialog = false 
                }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 引导到设置页面对话框
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("开启相机权限") },
            text = { Text("请在应用设置中开启相机权限，以使用拍照识别功能。") },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    // 跳转到应用设置页面
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 录音权限解释对话框
    if (showAudioPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showAudioPermissionDialog = false },
            title = { Text("需要录音权限") },
            text = { Text("语音输入功能需要使用麦克风，请授予录音权限。如果您之前拒绝了权限，可能需要在设置中手动开启。") },
            confirmButton = {
                TextButton(onClick = {
                    showAudioPermissionDialog = false
                    // 跳转到应用设置页面
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioPermissionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 图片选择底部菜单
    if (showImagePickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImagePickerSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "选择图片来源",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                
                // 从相册选择
                ImagePickerOption(
                    icon = Icons.Default.Image,
                    title = "从相册选择",
                    onClick = {
                        showImagePickerSheet = false
                        pickImageLauncher.launch("image/*")
                    }
                )
                
                // 拍照
                ImagePickerOption(
                    icon = Icons.Default.CameraAlt,
                    title = "拍照",
                    onClick = {
                        showImagePickerSheet = false
                        requestCameraPermission()
                    }
                )
            }
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("极速精灵", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "聊天记录")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        bottomBar = {
            ChatInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        val messageToSend = inputText
                        inputText = ""
                        viewModel.sendMessage(messageToSend) {
                            coroutineScope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        }
                    }
                },
                isLoading = isLoading,
                isOcrProcessing = isOcrProcessing,
                isQuickAddMode = isQuickAddMode,
                onQuickAddToggle = { isQuickAddMode = !isQuickAddMode },
                onImageClick = { showImagePickerSheet = true },
                isRecording = isRecording,
                recordingText = when (val state = speechState) {
                    is SparkChainSpeechHelper.RecognitionState.Result -> state.text
                    is SparkChainSpeechHelper.RecognitionState.Listening -> "正在聆听..."
                    else -> ""
                },
                speechVolume = speechVolume,
                onVoiceStart = {
                    if (!speechHelper.isAvailable()) {
                        Toast.makeText(context, "语音识别未配置，请联系开发者", Toast.LENGTH_SHORT).show()
                        return@ChatInputBar
                    }
                    requestAudioPermissionAndStart()
                },
                onVoiceEnd = {
                    isRecording = false
                    speechHelper.stopListening()
                },
                onVoiceCancel = {
                    isRecording = false
                    speechHelper.cancel()
                    Toast.makeText(context, "已取消", Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (messages.isEmpty() && !isLoading) {
                InitialChatView()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatHistoryDrawer(
    viewModel: AIChatViewModel,
    onSessionClick: (String) -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("聊天记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.createNewSession() }) {
                    Icon(Icons.Default.Add, contentDescription = "新建聊天")
                }
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sessions) { session ->
                    SessionItem(session = session, isSelected = session.id == currentSessionId) {
                        viewModel.loadChatHistory(session.id)
                        onSessionClick(session.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionItem(session: ChatSession, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(session.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(session.lastMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(formatTimestamp(session.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun InitialChatView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.1f))
                .padding(20.dp),
            tint = PrimaryBlue
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "你好，有什么可以帮到你？",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "你可以问我任何问题，或者让我帮你处理日程。",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Avatar",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(6.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            ),
            color = if (isUser) PrimaryBlue else MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            SelectionContainer {
                Text(
                    text = message.content,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    isOcrProcessing: Boolean = false,
    isQuickAddMode: Boolean = false,
    onQuickAddToggle: () -> Unit = {},
    onImageClick: () -> Unit = {},
    isRecording: Boolean = false,
    recordingText: String = "",
    speechVolume: Int = 0,
    onVoiceStart: () -> Unit = {},
    onVoiceEnd: () -> Unit = {},
    onVoiceCancel: () -> Unit = {}
) {
    // 语音按钮动画
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1.2f + (speechVolume * 0.03f) else 1f,
        animationSpec = tween(100),
        label = "voice_scale"
    )
    
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 录音状态提示
            if (isRecording) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlue.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (recordingText.isNotEmpty()) recordingText else "正在聆听...",
                        color = PrimaryBlue,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // 快速添加按钮行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    onClick = onQuickAddToggle,
                    shape = RoundedCornerShape(20.dp),
                    color = if (isQuickAddMode) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isQuickAddMode) Color.White else PrimaryBlue
                        )
                        Text(
                            text = "快速添加",
                            fontSize = 13.sp,
                            color = if (isQuickAddMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 输入栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 快速添加模式下显示图片上传按钮
                if (isQuickAddMode) {
                    IconButton(
                        onClick = onImageClick,
                        enabled = !isOcrProcessing,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.1f))
                    ) {
                        if (isOcrProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PrimaryBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "上传图片",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // 文本输入框 - 支持多行和滚动
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 120.dp)
                        .background(Background, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = if (isOcrProcessing) "正在识别..." else value,
                        onValueChange = onValueChange,
                        enabled = !isOcrProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = if (isOcrProcessing) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else 
                                MaterialTheme.colorScheme.onBackground
                        ),
                        decorationBox = { innerTextField ->
                            if (value.isEmpty() && !isOcrProcessing && !isRecording) {
                                Text("输入消息...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            innerTextField()
                        }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                // 语音输入按钮
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(if (isRecording) PrimaryBlue else PrimaryBlue.copy(alpha = 0.1f))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    // 按下开始录音
                                    onVoiceStart()
                                    // 等待释放
                                    val released = tryAwaitRelease()
                                    if (released) {
                                        // 正常释放，发送
                                        onVoiceEnd()
                                    } else {
                                        // 取消
                                        onVoiceCancel()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "语音输入",
                        tint = if (isRecording) Color.White else PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                val canSend = value.isNotBlank() && !isLoading && !isOcrProcessing && !isRecording
                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (canSend) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 图片选择器选项
 */
@Composable
private fun ImagePickerOption(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.1f))
                .padding(8.dp),
            tint = PrimaryBlue
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
