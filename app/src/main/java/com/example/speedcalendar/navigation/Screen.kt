package com.example.speedcalendar.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "首页")
    object AI : Screen("ai", "AI")
    object Mine : Screen("mine", "我的")
    object PersonalSettings : Screen("personal_settings", "个人设置")
    object EditProfile : Screen("edit_profile", "编辑资料")
    object PrivacySettings : Screen("privacy_settings", "隐私设置")
}