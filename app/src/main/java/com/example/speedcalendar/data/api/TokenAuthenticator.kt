package com.example.speedcalendar.data.api

import android.util.Log
import com.example.speedcalendar.data.local.UserPreferences
import com.example.speedcalendar.data.model.ApiResponse
import com.example.speedcalendar.data.model.RefreshTokenRequest
import com.example.speedcalendar.data.model.RefreshTokenResponse
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Token认证器
 * 当收到401响应时，自动使用refreshToken获取新的accessToken
 */
class TokenAuthenticator(
    private val userPreferences: UserPreferences
) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
//        private const val BASE_URL = "http://localhost:8080/api/"
        private const val BASE_URL = "http://122.51.127.61:8080/api/"

        // 防止无限循环刷新
        private const val MAX_RETRY_COUNT = 1
    }

    private val gson = Gson()

    // 用于刷新Token的独立OkHttpClient（不带Authenticator，避免循环）
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d(TAG, "authenticate: 收到401响应，尝试刷新Token")

        // 检查是否已经重试过（防止无限循环）
        val retryCount = response.request.header("Retry-Count")?.toIntOrNull() ?: 0
        if (retryCount >= MAX_RETRY_COUNT) {
            Log.w(TAG, "authenticate: 已达到最大重试次数，放弃刷新")
            return null
        }

        // 获取refreshToken
        val refreshToken = userPreferences.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            Log.w(TAG, "authenticate: 没有refreshToken，无法刷新")
            // 清除登录状态，触发重新登录
            userPreferences.clearLoginInfo()
            return null
        }

        // 调用刷新Token接口
        val newTokens = refreshTokenSync(refreshToken)
        if (newTokens == null) {
            Log.e(TAG, "authenticate: 刷新Token失败")
            // refreshToken也过期了，清除登录状态
            userPreferences.clearLoginInfo()
            return null
        }

        Log.d(TAG, "authenticate: 刷新Token成功，保存新Token")
        
        // 保存新Token
        userPreferences.updateToken(
            accessToken = newTokens.token,
            refreshToken = newTokens.refreshToken,
            expiresIn = newTokens.expiresIn
        )

        // 使用新Token重新发送请求
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.token}")
            .header("Retry-Count", (retryCount + 1).toString())
            .build()
    }

    /**
     * 同步调用刷新Token接口
     */
    private fun refreshTokenSync(refreshToken: String): RefreshTokenResponse? {
        return try {
            val requestBody = gson.toJson(RefreshTokenRequest(refreshToken))
                .toRequestBody("application/json".toMediaType())

            val request = okhttp3.Request.Builder()
                .url("${BASE_URL}auth/refresh")
                .post(requestBody)
                .build()

            val response = refreshClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "refreshTokenSync: 响应=$responseBody")
                
                if (responseBody != null) {
                    val apiResponse = gson.fromJson(
                        responseBody,
                        object : com.google.gson.reflect.TypeToken<ApiResponse<RefreshTokenResponse>>() {}.type
                    ) as ApiResponse<RefreshTokenResponse>
                    
                    if (apiResponse.code == 200 && apiResponse.data != null) {
                        apiResponse.data
                    } else {
                        Log.e(TAG, "refreshTokenSync: 业务失败 code=${apiResponse.code}, message=${apiResponse.message}")
                        null
                    }
                } else {
                    null
                }
            } else {
                Log.e(TAG, "refreshTokenSync: HTTP失败 code=${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "refreshTokenSync: 异常", e)
            null
        }
    }
}
