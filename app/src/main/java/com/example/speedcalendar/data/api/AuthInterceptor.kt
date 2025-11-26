package com.example.speedcalendar.data.api

import android.util.Log
import com.example.speedcalendar.data.local.UserPreferences
import com.example.speedcalendar.data.model.ApiResponse
import com.example.speedcalendar.data.model.RefreshTokenRequest
import com.example.speedcalendar.data.model.RefreshTokenResponse
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.TimeUnit

/**
 * 认证拦截器
 * 1. 自动在请求头中添加Authorization Token
 * 2. 检查响应体中的code，如果是401则自动刷新Token并重试
 */
class AuthInterceptor(
    private val userPreferences: UserPreferences
) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        private const val BASE_URL = "http://localhost:8080/api/"
        private const val HEADER_RETRY = "X-Token-Retry"
    }

    private val gson = Gson()

    // 用于刷新Token的独立OkHttpClient（不带拦截器，避免循环）
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 构建带Token的请求
        val token = userPreferences.getAccessToken()
        val request = if (!token.isNullOrEmpty() && originalRequest.header("Authorization") == null) {
            Log.d(TAG, "intercept: 添加Authorization头")
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        // 执行请求
        val response = chain.proceed(request)

        // 如果是刷新Token的请求，不检查响应体（避免循环）
        if (request.url.encodedPath.contains("auth/refresh")) {
            return response
        }

        // 如果已经重试过，不再重试
        if (request.header(HEADER_RETRY) != null) {
            return response
        }

        // 读取响应体检查业务code
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            return response
        }

        // 尝试解析业务code
        val businessCode = try {
            val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)
            apiResponse?.code
        } catch (e: Exception) {
            null
        }

        Log.d(TAG, "intercept: 业务code=$businessCode")

        // 如果业务code是401，尝试刷新Token
        if (businessCode == 401) {
            Log.d(TAG, "intercept: 检测到业务code=401，尝试刷新Token")
            
            val refreshToken = userPreferences.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                Log.w(TAG, "intercept: 没有refreshToken，无法刷新")
                userPreferences.clearLoginInfo()
                // 返回原响应（需要重新构建，因为body已被消费）
                return response.newBuilder()
                    .body(responseBody.toResponseBody(response.body?.contentType()))
                    .build()
            }

            // 刷新Token
            val newTokens = refreshTokenSync(refreshToken)
            if (newTokens == null) {
                Log.e(TAG, "intercept: 刷新Token失败")
                userPreferences.clearLoginInfo()
                return response.newBuilder()
                    .body(responseBody.toResponseBody(response.body?.contentType()))
                    .build()
            }

            Log.d(TAG, "intercept: 刷新Token成功，保存新Token并重试请求")
            
            // 保存新Token
            userPreferences.updateToken(
                accessToken = newTokens.token,
                refreshToken = newTokens.refreshToken,
                expiresIn = newTokens.expiresIn
            )

            // 关闭原响应
            response.close()

            // 使用新Token重试请求
            val retryRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer ${newTokens.token}")
                .header(HEADER_RETRY, "true")
                .build()

            return chain.proceed(retryRequest)
        }

        // 返回响应（需要重新构建，因为body已被消费）
        return response.newBuilder()
            .body(responseBody.toResponseBody(response.body?.contentType()))
            .build()
    }

    /**
     * 同步调用刷新Token接口
     */
    private fun refreshTokenSync(refreshToken: String): RefreshTokenResponse? {
        return try {
            Log.d(TAG, "refreshTokenSync: 开始刷新Token")
            
            val requestBody = gson.toJson(RefreshTokenRequest(refreshToken))
                .toRequestBody("application/json".toMediaType())

            val request = okhttp3.Request.Builder()
                .url("${BASE_URL}auth/refresh")
                .post(requestBody)
                .build()

            val response = refreshClient.newCall(request).execute()
            
            val responseBodyStr = response.body?.string()
            Log.d(TAG, "refreshTokenSync: HTTP=${response.code}, 响应=$responseBodyStr")

            if (response.isSuccessful && responseBodyStr != null) {
                val apiResponse = gson.fromJson(
                    responseBodyStr,
                    object : com.google.gson.reflect.TypeToken<ApiResponse<RefreshTokenResponse>>() {}.type
                ) as ApiResponse<RefreshTokenResponse>
                
                if (apiResponse.code == 200 && apiResponse.data != null) {
                    Log.d(TAG, "refreshTokenSync: 刷新成功")
                    apiResponse.data
                } else {
                    Log.e(TAG, "refreshTokenSync: 业务失败 code=${apiResponse.code}, message=${apiResponse.message}")
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
