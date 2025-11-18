package com.example.speedcalendar.features.mine.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.speedcalendar.ui.theme.LightBlueSurface
import com.example.speedcalendar.ui.theme.PrimaryBlue
import com.example.speedcalendar.viewmodel.AuthViewModel
import com.example.speedcalendar.viewmodel.AvatarViewModel

/**
 * 编辑资料页面
 *
 * TODO: 添加数据刷新机制
 * 当前问题：
 * 1. 页面打开时不刷新：只显示本地缓存数据，可能过期
 * 2. 无手动刷新：用户无法主动拉取最新数据
 * 3. 多设备不同步：其他设备修改后，当前设备看不到更新
 *
 * 改进建议：
 * 方案1：进入页面时自动刷新
 *   LaunchedEffect(Unit) {
 *       userInfo?.userId?.let { userId ->
 *           viewModel.fetchUserInfoFromServer(userId) // 需要添加此方法
 *       }
 *   }
 *
 * 方案2：下拉刷新
 *   - 使用 SwipeRefresh 或 PullRefreshIndicator
 *   - 用户下拉时重新获取数据
 *   - 提供明确的刷新反馈
 *
 * 方案3：定时刷新
 *   - 每次从后台切到前台时刷新
 *   - 使用 Lifecycle 监听应用状态
 *
 * 示例实现：
 * ```kotlin
 * val pullRefreshState = rememberPullRefreshState(
 *     refreshing = isRefreshing,
 *     onRefresh = {
 *         userInfo?.userId?.let { viewModel.fetchUserInfoFromServer(it) }
 *     }
 * )
 * Box(Modifier.pullRefresh(pullRefreshState)) {
 *     // 现有内容
 *     PullRefreshIndicator(isRefreshing, pullRefreshState)
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
    avatarViewModel: AvatarViewModel = viewModel()
) {
    val context = LocalContext.current
    val userInfo by viewModel.userInfo.collectAsState()
    val isUploading by avatarViewModel.isUploading.collectAsState()
    val uploadProgress by avatarViewModel.uploadProgress.collectAsState()
    val errorMessage by avatarViewModel.errorMessage.collectAsState()
    val successMessage by avatarViewModel.successMessage.collectAsState()

    var nickname by remember { mutableStateOf(userInfo?.username ?: "") }
    var showNicknameDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val userId = userInfo?.userId
            if (userId != null) {
                avatarViewModel.uploadAvatar(userId, it)
            }
        }
    }

    LaunchedEffect(userInfo) {
        nickname = userInfo?.username ?: ""
        // 调试：打印头像URL
        android.util.Log.d("EditProfileScreen", "用户信息更新: avatar=${userInfo?.avatar}")
    }

    // 显示错误消息
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            avatarViewModel.clearErrorMessage()
        }
    }

    // 显示成功消息并刷新用户信息
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            avatarViewModel.clearSuccessMessage()
            // 刷新用户信息以更新头像
            viewModel.refreshUserInfo()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "编辑资料",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.padding(end = 48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 头像区域
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(if (userInfo?.avatar.isNullOrEmpty()) PrimaryBlue else Color.Transparent)
                            .border(2.dp, PrimaryBlue.copy(alpha = 0.2f), CircleShape)
                            .clickable(enabled = !isUploading) {
                                imagePickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!userInfo?.avatar.isNullOrEmpty()) {
                            // 使用Coil加载头像
                            val avatarUrl = userInfo?.avatar
                            android.util.Log.d("EditProfileScreen", "正在加载头像: $avatarUrl")

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarUrl)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .networkCachePolicy(CachePolicy.ENABLED)
                                    .listener(
                                        onError = { _, result ->
                                            android.util.Log.e("EditProfileScreen", "头像加载失败: ${result.throwable.message}")
                                        },
                                        onSuccess = { _, _ ->
                                            android.util.Log.d("EditProfileScreen", "头像加载成功")
                                        }
                                    )
                                    .build(),
                                contentDescription = "头像",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "默认头像",
                                tint = Color.White,
                                modifier = Modifier.size(60.dp)
                            )
                        }

                        // 上传进度遮罩
                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = uploadProgress / 100f,
                                    color = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    // 相机图标
                    if (!isUploading) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, PrimaryBlue.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "更换头像",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isUploading) "上传中 $uploadProgress%" else "点击更换头像",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 个人信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LightBlueSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    // 昵称
                    ProfileEditItem(
                        label = "昵称",
                        value = nickname,
                        onClick = { showNicknameDialog = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = PrimaryBlue.copy(alpha = 0.1f)
                    )

                    // 手机号（不可编辑）
                    ProfileInfoItem(
                        label = "手机号",
                        value = userInfo?.phone ?: ""
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 说明文字
            Text(
                text = "温馨提示：头像和昵称将在群体日程中展示给其他成员",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    // 编辑昵称对话框
    if (showNicknameDialog) {
        EditNicknameDialog(
            currentNickname = nickname,
            onDismiss = { showNicknameDialog = false },
            onConfirm = { newNickname ->
                // 调用 API 更新昵称
                val userId = userInfo?.userId
                if (userId != null) {
                    viewModel.updateUserInfo(userId, username = newNickname)
                    nickname = newNickname
                }
                showNicknameDialog = false
            }
        )
    }
}

/**
 * 可编辑的资料项
 */
@Composable
private fun ProfileEditItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(80.dp)
        )

        Text(
            text = value.ifEmpty { "未设置" },
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = 180f }
        )
    }
}

/**
 * 不可编辑的信息项
 */
@Composable
private fun ProfileInfoItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(80.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 编辑昵称对话框
 */
@Composable
private fun EditNicknameDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentNickname) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改昵称") },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = { if (it.length <= 20) text = it },
                    singleLine = true,
                    placeholder = { Text("请输入昵称") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF7F8FA),
                        unfocusedContainerColor = Color(0xFFF7F8FA),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${text.length}/20",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotEmpty()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
