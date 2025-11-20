package com.example.speedcalendar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.speedcalendar.features.ai.AIScreen
import com.example.speedcalendar.features.ai.chat.AIChatScreen
import com.example.speedcalendar.features.home.HomeScreen
import com.example.speedcalendar.features.mine.MineScreen
import com.example.speedcalendar.features.mine.settings.PersonalSettingsScreen
import com.example.speedcalendar.features.mine.settings.EditProfileScreen
import com.example.speedcalendar.features.mine.settings.PrivacySettingsScreen
import com.example.speedcalendar.navigation.Screen
import com.example.speedcalendar.viewmodel.AuthViewModel
import java.net.URLDecoder

/**
 * TODO: 应用启动和前后台切换时刷新数据
 * 当前问题：
 * 1. 冷启动只读缓存：应用启动时不从后端获取最新数据
 * 2. 后台返回不刷新：从后台切到前台时不更新数据
 * 3. 数据可能过期：长时间不使用后，本地数据与后端不一致
 *
 * 改进建议：
 * 方案1：应用启动时刷新
 *   LaunchedEffect(Unit) {
 *       val lastUpdateTime = userPreferences.getLastUpdateTime()
 *       if (System.currentTimeMillis() - lastUpdateTime > REFRESH_THRESHOLD) {
 *           authViewModel.fetchUserInfoFromServer()
 *       }
 *   }
 *
 * 方案2：监听生命周期
 *   val lifecycleOwner = LocalLifecycleOwner.current
 *   DisposableEffect(lifecycleOwner) {
 *       val observer = LifecycleEventObserver { _, event ->
 *           if (event == Lifecycle.Event.ON_RESUME) {
 *               authViewModel.refreshIfNeeded()
 *           }
 *       }
 *       lifecycleOwner.lifecycle.addObserver(observer)
 *       onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
 *   }
 *
 * 方案3：智能刷新策略
 *   - 短时间内（5分钟）返回前台：不刷新
 *   - 长时间（1小时）返回前台：自动刷新
 *   - 首次启动：强制刷新
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    // 在 MainScreen 级别创建共享的 AuthViewModel
    val authViewModel: AuthViewModel = viewModel()

    // TODO: 在这里添加生命周期监听和刷新逻辑
    // 示例：
    // val lifecycleOwner = LocalLifecycleOwner.current
    // DisposableEffect(lifecycleOwner) { ... }

    Scaffold(
        bottomBar = {
            BottomNavigationBar {
                val items = listOf(Screen.Home, Screen.AI, Screen.Mine)
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    CustomTab(
                        screen = screen,
                        currentRoute = currentRoute
                    ) {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Home.route,
            // **关键改动**: 创建一个新的 PaddingValues，忽略顶部的 padding
            Modifier.padding(PaddingValues(
                start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                bottom = innerPadding.calculateBottomPadding()))
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.AI.route) {
                AIScreen(
                    onNavigateToChat = { initialMessage ->
                        navController.navigate(Screen.AIChat.createRoute(initialMessage))
                    }
                )
            }
            composable(
                route = Screen.AIChat.route,
                arguments = listOf(
                    navArgument("initialMessage") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val initialMessage = backStackEntry.arguments?.getString("initialMessage")?.let {
                    URLDecoder.decode(it, "UTF-8")
                }
                AIChatScreen(
                    initialMessage = initialMessage,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Mine.route) {
                MineScreen(
                    onNavigateToPersonalSettings = {
                        navController.navigate(Screen.PersonalSettings.route)
                    },
                    viewModel = authViewModel // 传递共享的 ViewModel
                )
            }
            composable(Screen.PersonalSettings.route) {
                PersonalSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onEditProfile = {
                        navController.navigate(Screen.EditProfile.route)
                    },
                    onPrivacySettings = {
                        navController.navigate(Screen.PrivacySettings.route)
                    },
                    viewModel = authViewModel // 传递共享的 ViewModel
                )
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = authViewModel // 传递共享的 ViewModel
                )
            }
            composable(Screen.PrivacySettings.route) {
                PrivacySettingsScreen(
                    onBack = { navController.popBackStack() },
                    authViewModel = authViewModel // 传递共享的 ViewModel
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(content: @Composable RowScope.() -> Unit) {
    Surface(shadowElevation = 8.dp) { // Optional: adds a shadow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Standard height for bottom navigation
            content = content
        )
    }
}

@Composable
private fun RowScope.CustomTab(
    screen: Screen,
    currentRoute: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f) // This works because CustomTab is a RowScope extension
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Disable ripple
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = screen.title,
            color = if (currentRoute == screen.route) Color.Black else Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}