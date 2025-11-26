package com.example.speedcalendar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.speedcalendar.features.mine.settings.EditProfileScreen
import com.example.speedcalendar.features.mine.settings.PersonalSettingsScreen
import com.example.speedcalendar.features.mine.settings.PrivacySettingsScreen
import com.example.speedcalendar.navigation.Screen
import com.example.speedcalendar.ui.theme.Background
import com.example.speedcalendar.ui.theme.PrimaryBlue
import com.example.speedcalendar.viewmodel.AuthViewModel
import java.net.URLDecoder

data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainScreen(
    onRequestScreenCapture: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    val bottomNavItems = listOf(
        BottomNavItem("日历", Screen.Home.route, Icons.Filled.DateRange, Icons.Outlined.DateRange),
        BottomNavItem("AI助手", Screen.AI.route, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
        BottomNavItem("我的", Screen.Mine.route, Icons.Filled.Person, Icons.Outlined.PersonOutline)
    )

    Scaffold(
        containerColor = Background,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) { HomeScreen() }
                composable(Screen.AI.route) {
                    AIScreen(
                        onNavigateToChat = { initialMessage ->
                            navController.navigate(Screen.AIChat.createRoute(initialMessage))
                        },
                        onRequestScreenCapture = onRequestScreenCapture
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
                        viewModel = authViewModel
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
                        viewModel = authViewModel
                    )
                }
                composable(Screen.EditProfile.route) {
                    EditProfileScreen(
                        onBack = { navController.popBackStack() },
                        viewModel = authViewModel
                    )
                }
                composable(Screen.PrivacySettings.route) {
                    PrivacySettingsScreen(
                        onBack = { navController.popBackStack() },
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}
