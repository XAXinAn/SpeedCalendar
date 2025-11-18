package com.example.speedcalendar.features.mine.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speedcalendar.data.model.PrivacySettingDTO
import com.example.speedcalendar.data.model.VisibilityLevel
import com.example.speedcalendar.data.model.VisibilityOption
import com.example.speedcalendar.ui.theme.LightBlueSurface
import com.example.speedcalendar.ui.theme.PrimaryBlue
import com.example.speedcalendar.viewmodel.AuthViewModel
import com.example.speedcalendar.viewmodel.PrivacyViewModel

/**
 * 隐私设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    privacyViewModel: PrivacyViewModel = viewModel()
) {
    val userInfo by authViewModel.userInfo.collectAsState()
    val privacySettings by privacyViewModel.privacySettings.collectAsState()
    val isLoading by privacyViewModel.isLoading.collectAsState()
    val successMessage by privacyViewModel.successMessage.collectAsState()
    val errorMessage by privacyViewModel.errorMessage.collectAsState()

    // 本地编辑状态
    var localSettings by remember { mutableStateOf<List<PrivacySettingDTO>>(emptyList()) }
    var showDialog by remember { mutableStateOf<PrivacySettingDTO?>(null) }
    var hasChanges by remember { mutableStateOf(false) }

    // 加载数据
    LaunchedEffect(userInfo) {
        userInfo?.userId?.let { userId ->
            privacyViewModel.loadPrivacySettings(userId)
        }
    }

    // 同步服务器数据到本地
    LaunchedEffect(privacySettings) {
        if (privacySettings.isNotEmpty()) {
            localSettings = privacySettings
        }
    }

    // 显示成功消息
    LaunchedEffect(successMessage) {
        successMessage?.let {
            // 这里可以显示一个Snackbar
            privacyViewModel.clearSuccessMessage()
            hasChanges = false
        }
    }

    // 显示错误消息
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // 这里可以显示一个Snackbar
            privacyViewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "隐私设置",
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
                    // 保存按钮
                    TextButton(
                        onClick = {
                            userInfo?.userId?.let { userId ->
                                privacyViewModel.updatePrivacySettings(userId, localSettings)
                            }
                        },
                        enabled = hasChanges && !isLoading
                    ) {
                        Text(
                            text = "保存",
                            color = if (hasChanges) PrimaryBlue else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && localSettings.isEmpty()) {
                // 加载中
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // 说明文字
                    item {
                        Text(
                            text = "管理谁可以查看你的个人信息",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // 隐私设置列表
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = LightBlueSurface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column {
                                localSettings.forEachIndexed { index, setting ->
                                    PrivacySettingItem(
                                        setting = setting,
                                        onClick = { showDialog = setting }
                                    )

                                    if (index < localSettings.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = PrimaryBlue.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 底部说明
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "注意：\"仅好友可见\"功能即将推出，当前等同于\"私密\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }

    // 选择可见性对话框
    showDialog?.let { setting ->
        VisibilitySelectionDialog(
            setting = setting,
            onDismiss = { showDialog = null },
            onConfirm = { newLevel ->
                // 更新本地设置
                localSettings = localSettings.map {
                    if (it.fieldName == setting.fieldName) {
                        it.copy(visibilityLevel = newLevel)
                    } else {
                        it
                    }
                }
                hasChanges = true
                showDialog = null
            }
        )
    }
}

/**
 * 隐私设置项
 */
@Composable
private fun PrivacySettingItem(
    setting: PrivacySettingDTO,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = getVisibilityLabel(setting.visibilityLevel),
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryBlue,
            modifier = Modifier.padding(end = 8.dp)
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
 * 可见性选择对话框
 */
@Composable
private fun VisibilitySelectionDialog(
    setting: PrivacySettingDTO,
    onDismiss: () -> Unit,
    onConfirm: (VisibilityLevel) -> Unit
) {
    var selectedLevel by remember { mutableStateOf(setting.visibilityLevel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${setting.displayName}可见性") },
        text = {
            Column {
                VisibilityOption.options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLevel = option.level }
                            .background(
                                if (selectedLevel == option.level) PrimaryBlue.copy(alpha = 0.1f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLevel == option.level,
                            onClick = { selectedLevel = option.level },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryBlue
                            )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedLevel == option.level) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedLevel) }) {
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

/**
 * 获取可见性级别的显示文本
 */
private fun getVisibilityLabel(level: VisibilityLevel): String {
    return when (level) {
        VisibilityLevel.PUBLIC -> "公开"
        VisibilityLevel.FRIENDS_ONLY -> "仅好友"
        VisibilityLevel.PRIVATE -> "私密"
    }
}
