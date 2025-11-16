package com.example.speedcalendar.features.mine.settings

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 设置项类型
 */
sealed class SettingsItemType {
    /**
     * 导航项（点击跳转到新页面）
     */
    data class Navigation(
        val icon: ImageVector? = null,
        val onClick: () -> Unit
    ) : SettingsItemType()

    /**
     * 开关项
     */
    data class Switch(
        val icon: ImageVector? = null,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingsItemType()

    /**
     * 文本项（仅显示信息）
     */
    data class Text(
        val icon: ImageVector? = null,
        val value: String = ""
    ) : SettingsItemType()

    /**
     * 操作项（如退出登录）
     */
    data class Action(
        val icon: ImageVector? = null,
        val onClick: () -> Unit,
        val dangerous: Boolean = false // 标记危险操作（如删除、退出）
    ) : SettingsItemType()
}

/**
 * 设置项
 */
data class SettingsItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val type: SettingsItemType
)

/**
 * 设置区块（用于分组）
 */
data class SettingsSection(
    val id: String,
    val title: String? = null,
    val items: List<SettingsItem>
)
