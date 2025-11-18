package com.example.speedcalendar.data.model

/**
 * 隐私可见性级别
 */
enum class VisibilityLevel {
    PUBLIC,        // 公开
    FRIENDS_ONLY,  // 仅好友可见（预留）
    PRIVATE        // 私密
}

/**
 * 隐私字段
 */
enum class PrivacyField(
    val fieldName: String,
    val displayName: String,
    val description: String
) {
    PHONE("phone", "手机号", "你的手机号码"),
    EMAIL("email", "邮箱", "你的电子邮箱"),
    BIRTHDAY("birthday", "生日", "你的生日信息"),
    GENDER("gender", "性别", "你的性别信息"),
    BIO("bio", "个人简介", "你的个人简介")
}

/**
 * 隐私设置DTO
 */
data class PrivacySettingDTO(
    val fieldName: String,
    val displayName: String,
    val visibilityLevel: VisibilityLevel
)

/**
 * 批量更新隐私设置请求
 */
data class UpdatePrivacySettingsRequest(
    val settings: List<PrivacySettingDTO>
)

/**
 * 可见性级别选项（用于UI显示）
 */
data class VisibilityOption(
    val level: VisibilityLevel,
    val label: String,
    val description: String
) {
    companion object {
        val options = listOf(
            VisibilityOption(
                VisibilityLevel.PUBLIC,
                "公开",
                "所有人可见"
            ),
            VisibilityOption(
                VisibilityLevel.FRIENDS_ONLY,
                "仅好友可见",
                "仅你的好友可见（即将推出）"
            ),
            VisibilityOption(
                VisibilityLevel.PRIVATE,
                "私密",
                "仅自己可见"
            )
        )
    }
}
