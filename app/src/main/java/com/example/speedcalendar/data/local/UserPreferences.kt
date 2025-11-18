package com.example.speedcalendar.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.speedcalendar.data.model.LoginResponse
import com.example.speedcalendar.data.model.UserInfo
import com.google.gson.Gson

/**
 * 用户本地存储
 * 使用SharedPreferences保存用户登录信息和Token
 *
 * TODO: 缓存过期机制
 * 当前问题：
 * 1. 没有缓存时间戳：无法判断本地数据是否过期
 * 2. 没有过期检查：数据可能长期不更新，与后端不一致
 * 3. 没有版本控制：无法追踪数据更新历史
 *
 * 改进建议：
 * 方案1：添加时间戳字段
 *   - 新增 KEY_USER_INFO_TIMESTAMP 保存最后更新时间
 *   - getUserInfo()时检查时间，超过阈值（如1小时）返回null
 *   - 强制应用从后端重新获取
 *
 * 方案2：添加数据版本号
 *   - 新增 KEY_USER_INFO_VERSION 字段
 *   - 后端API返回version字段
 *   - 每次请求时对比版本，不一致则刷新
 *
 * 方案3：缓存策略配置
 *   - 支持配置缓存时长（用户设置、隐私设置等不同时长）
 *   - 重要数据短缓存，不常变数据长缓存
 *
 * 示例实现：
 * ```kotlin
 * private const val KEY_USER_INFO_TIMESTAMP = "user_info_timestamp"
 * private const val CACHE_VALID_DURATION = 60 * 60 * 1000L // 1小时
 *
 * fun getUserInfo(): UserInfo? {
 *     val timestamp = prefs.getLong(KEY_USER_INFO_TIMESTAMP, 0)
 *     if (System.currentTimeMillis() - timestamp > CACHE_VALID_DURATION) {
 *         return null // 缓存过期，需要重新获取
 *     }
 *     val json = prefs.getString(KEY_USER_INFO, null)
 *     return json?.let { gson.fromJson(it, UserInfo::class.java) }
 * }
 * ```
 */
class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "speed_calendar_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRES_IN = "token_expires_in"
        private const val KEY_USER_INFO = "user_info"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        // TODO: 添加时间戳和版本号字段
        // private const val KEY_USER_INFO_TIMESTAMP = "user_info_timestamp"
        // private const val KEY_USER_INFO_VERSION = "user_info_version"

        @Volatile
        private var instance: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return instance ?: synchronized(this) {
                instance ?: UserPreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 保存登录信息
     */
    fun saveLoginInfo(loginResponse: LoginResponse) {
        prefs.edit().apply {
            putString(KEY_USER_ID, loginResponse.userId)
            putString(KEY_ACCESS_TOKEN, loginResponse.token)
            putString(KEY_REFRESH_TOKEN, loginResponse.refreshToken)
            putLong(KEY_TOKEN_EXPIRES_IN, loginResponse.expiresIn)
            putString(KEY_USER_INFO, gson.toJson(loginResponse.userInfo))
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * 获取用户ID
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * 获取AccessToken
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * 获取RefreshToken
     */
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * 获取用户信息
     */
    fun getUserInfo(): UserInfo? {
        val userInfoJson = prefs.getString(KEY_USER_INFO, null) ?: return null
        return try {
            gson.fromJson(userInfoJson, UserInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getAccessToken() != null
    }

    /**
     * 清除登录信息（登出）
     */
    fun clearLoginInfo() {
        prefs.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_EXPIRES_IN)
            remove(KEY_USER_INFO)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    /**
     * 更新Token
     */
    fun updateToken(accessToken: String, refreshToken: String, expiresIn: Long) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_TOKEN_EXPIRES_IN, expiresIn)
            apply()
        }
    }

    /**
     * 更新用户信息
     * 使用commit()同步写入，确保立即生效
     */
    fun updateUserInfo(userInfo: UserInfo) {
        prefs.edit().apply {
            putString(KEY_USER_INFO, gson.toJson(userInfo))
            commit() // 改为commit()同步写入，保证立即生效
        }
    }
}
