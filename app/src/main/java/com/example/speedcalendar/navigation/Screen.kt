package com.example.speedcalendar.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "首页")
    object AI : Screen("ai", "AI")
    object Mine : Screen("mine", "我的")
}