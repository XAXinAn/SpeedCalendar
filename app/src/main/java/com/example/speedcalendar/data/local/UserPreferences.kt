package com.example.speedcalendar.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.speedcalendar.data.model.LoginResponse
import com.example.speedcalendar.data.model.UserInfo
import com.google.gson.Gson

/**
 * 用户本地存储
 * 使用SharedPreferences保存用户登录信息和Token
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
}
