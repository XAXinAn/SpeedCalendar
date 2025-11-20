package com.example.speedcalendar.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit客户端单例
 */
object RetrofitClient {

    /**
     * 后端服务器地址
     * 注意：Android模拟器访问本机需要使用 10.0.2.2
     * 真机 + USB 调试 + adb reverse 使用 localhost
     * 真机访问需要使用主机的实际IP地址
     */
    //private const val BASE_URL = "http://localhost:8080/api/"
    private const val BASE_URL = "http://10.0.2.2:8080/api/"

    /**
     * OkHttp客户端
     */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Retrofit实例
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * AuthApiService实例
     */
    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    /**
     * PrivacyApiService实例
     */
    val privacyApiService: PrivacyApiService by lazy {
        retrofit.create(PrivacyApiService::class.java)
    }

    /**
     * AvatarApiService实例
     */
    val avatarApiService: AvatarApiService by lazy {
        retrofit.create(AvatarApiService::class.java)
    }

    /**
     * AIChatApiService实例
     */
    val aiChatApiService: AIChatApiService by lazy {
        retrofit.create(AIChatApiService::class.java)
    }
}
