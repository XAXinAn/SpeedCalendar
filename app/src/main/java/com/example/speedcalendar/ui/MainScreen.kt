package com.example.speedcalendar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.speedcalendar.features.ai.AIScreen
import com.example.speedcalendar.features.home.HomeScreen
import com.example.speedcalendar.features.mine.MineScreen
import com.example.speedcalendar.navigation.Screen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
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
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.AI.route) { AIScreen() }
            composable(Screen.Mine.route) { MineScreen() }
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