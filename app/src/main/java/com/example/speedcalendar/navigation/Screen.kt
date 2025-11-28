package com.example.speedcalendar.navigation

import java.time.LocalDate

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "首页")
    object AI : Screen("ai", "AI")
    object Mine : Screen("mine", "我的")
    object PersonalSettings : Screen("personal_settings", "个人设置")
    object EditProfile : Screen("edit_profile", "编辑资料")
    object PrivacySettings : Screen("privacy_settings", "隐私设置")
    object GroupSettings : Screen("group_settings", "群组设置")
    object CreateGroup : Screen("create_group", "新建群组")
    object AIChat : Screen("ai_chat?initialMessage={initialMessage}", "AI聊天") {
        fun createRoute(initialMessage: String? = null): String {
            return if (initialMessage != null) {
                "ai_chat?initialMessage=${java.net.URLEncoder.encode(initialMessage, "UTF-8")}"
            } else {
                "ai_chat"
            }
        }
    }
    object AddSchedule : Screen("add_schedule/{selectedDate}", "添加日程") {
        fun createRoute(selectedDate: LocalDate): String {
            return "add_schedule/$selectedDate"
        }
    }
    object EditSchedule : Screen("edit_schedule/{scheduleId}", "编辑日程") {
        fun createRoute(scheduleId: String): String {
            return "edit_schedule/$scheduleId"
        }
    }
}