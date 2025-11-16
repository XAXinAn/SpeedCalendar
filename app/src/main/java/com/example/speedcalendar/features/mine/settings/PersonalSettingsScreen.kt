package com.example.speedcalendar.features.mine.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speedcalendar.viewmodel.AuthViewModel

/**
 * 个人设置页面
 * 企业级实践：
 * 1. 数据驱动：设置项通过数据结构定义，易于维护和扩展
 * 2. 组件化：使用可复用的 SettingsSectionView 组件
 * 3. 清晰分层：UI、数据、业务逻辑分离
 * 4. 易于扩展：添加新设置项只需修改 settingsSections 列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalSettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val userInfo by viewModel.userInfo.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // 定义设置区块（数据驱动，易于扩展）
    val settingsSections = remember(userInfo) {
        buildPersonalSettings(
            userInfo = userInfo,
            onEditProfile = onEditProfile,
            onLogout = { showLogoutDialog = true }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "个人设置",
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
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = 16.dp)
        ) {
            // 渲染所有设置区块
            settingsSections.forEach { section ->
                SettingsSectionView(
                    section = section,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    // 退出登录确认对话框
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onBack()
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 构建个人设置项列表
 * 企业级实践：使用 Builder 模式，易于扩展和维护
 */
private fun buildPersonalSettings(
    userInfo: com.example.speedcalendar.data.model.UserInfo?,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
): List<SettingsSection> {
    return listOf(
        // 账号信息区块
        SettingsSection(
            id = "account_info",
            title = "账号信息",
            items = listOf(
                SettingsItem(
                    id = "edit_profile",
                    title = "编辑资料",
                    subtitle = "修改头像、昵称等个人信息",
                    type = SettingsItemType.Navigation(
                        icon = Icons.Default.Person,
                        onClick = onEditProfile
                    )
                ),
                SettingsItem(
                    id = "phone",
                    title = "手机号",
                    type = SettingsItemType.Text(
                        icon = Icons.Default.Phone,
                        value = userInfo?.phone ?: "未绑定"
                    )
                )
            )
        ),

        // 隐私与安全区块（示例，可扩展）
        SettingsSection(
            id = "privacy_security",
            title = "隐私与安全",
            items = listOf(
                SettingsItem(
                    id = "password",
                    title = "修改密码",
                    subtitle = "定期更换密码以保护账号安全",
                    type = SettingsItemType.Navigation(
                        icon = Icons.Default.Lock,
                        onClick = { /* TODO: 导航到修改密码页面 */ }
                    )
                ),
                SettingsItem(
                    id = "privacy",
                    title = "隐私设置",
                    subtitle = "管理个人信息的可见范围",
                    type = SettingsItemType.Navigation(
                        icon = Icons.Default.Shield,
                        onClick = { /* TODO: 导航到隐私设置页面 */ }
                    )
                )
            )
        ),

        // 账号操作区块
        SettingsSection(
            id = "account_actions",
            title = null,
            items = listOf(
                SettingsItem(
                    id = "logout",
                    title = "退出登录",
                    type = SettingsItemType.Action(
                        icon = Icons.Default.Logout,
                        onClick = onLogout,
                        dangerous = true
                    )
                )
            )
        )
    )
}
