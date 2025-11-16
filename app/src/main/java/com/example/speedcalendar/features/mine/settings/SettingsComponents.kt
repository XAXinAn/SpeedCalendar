package com.example.speedcalendar.features.mine.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedcalendar.ui.theme.LightBlueSurface
import com.example.speedcalendar.ui.theme.PrimaryBlue

/**
 * 设置区块组件
 * 用于显示一组相关的设置项
 */
@Composable
fun SettingsSectionView(
    section: SettingsSection,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 区块标题（可选）
        section.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Medium
            )
        }

        // 设置项列表
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = LightBlueSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                section.items.forEachIndexed { index, item ->
                    SettingsItemView(item = item)

                    // 添加分割线（最后一项除外）
                    if (index < section.items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = PrimaryBlue.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 通用设置项组件
 * 根据类型自动渲染不同的样式
 */
@Composable
fun SettingsItemView(
    item: SettingsItem,
    modifier: Modifier = Modifier
) {
    when (val type = item.type) {
        is SettingsItemType.Navigation -> {
            NavigationSettingsItem(
                item = item,
                icon = type.icon,
                onClick = type.onClick,
                modifier = modifier
            )
        }
        is SettingsItemType.Switch -> {
            SwitchSettingsItem(
                item = item,
                icon = type.icon,
                checked = type.checked,
                onCheckedChange = type.onCheckedChange,
                modifier = modifier
            )
        }
        is SettingsItemType.Text -> {
            TextSettingsItem(
                item = item,
                icon = type.icon,
                value = type.value,
                modifier = modifier
            )
        }
        is SettingsItemType.Action -> {
            ActionSettingsItem(
                item = item,
                icon = type.icon,
                onClick = type.onClick,
                dangerous = type.dangerous,
                modifier = modifier
            )
        }
    }
}

/**
 * 导航型设置项
 */
@Composable
private fun NavigationSettingsItem(
    item: SettingsItem,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标（可选）
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        // 标题和副标题
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            item.subtitle?.let { subtitle ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 箭头
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 开关型设置项
 */
@Composable
private fun SwitchSettingsItem(
    item: SettingsItem,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标（可选）
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        // 标题和副标题
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            item.subtitle?.let { subtitle ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 开关
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

/**
 * 文本型设置项
 */
@Composable
private fun TextSettingsItem(
    item: SettingsItem,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标（可选）
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        // 标题
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // 值
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 操作型设置项（如退出登录）
 */
@Composable
private fun ActionSettingsItem(
    item: SettingsItem,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    dangerous: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (dangerous) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 图标（可选）
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        // 标题
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = if (dangerous) FontWeight.Medium else FontWeight.Normal
        )
    }
}
