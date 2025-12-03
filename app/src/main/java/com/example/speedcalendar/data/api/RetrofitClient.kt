package com.example.speedcalendar.data.api

import android.content.Context
import com.example.speedcalendar.data.local.UserPreferences
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
//    private const val BASE_URL = "http://192.168.43.227:8080/api/"
    //private const val BASE_URL = "http://10.0.2.2:8080/api/"
//      private const val BASE_URL = "http://localhost:8080/api/"
      private const val BASE_URL = "http://122.51.127.61:8080/api/"

    private var userPreferences: UserPreferences? = null
    private var okHttpClient: OkHttpClient? = null
    private var retrofit: Retrofit? = null

    /**
     * 初始化RetrofitClient
     * 必须在Application或第一次使用前调用
     */
    fun init(context: Context) {
        if (userPreferences == null) {
            userPreferences = UserPreferences.getInstance(context)
            okHttpClient = createOkHttpClient()
            retrofit = createRetrofit()
        }
    }

    /**
     * 创建OkHttp客户端
     */
    private fun createOkHttpClient(): OkHttpClient {
        val prefs = userPreferences ?: throw IllegalStateException("RetrofitClient未初始化，请先调用init()")
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(prefs)) // 自动添加Token + 401时自动刷新
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * 创建Retrofit实例
     */
    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient ?: throw IllegalStateException("OkHttpClient未初始化"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 获取Retrofit实例
     */
    private fun getRetrofit(): Retrofit {
        return retrofit ?: throw IllegalStateException("RetrofitClient未初始化，请先调用init()")
    }

    /**
     * AuthApiService实例
     */
    val authApiService: AuthApiService
        get() = getRetrofit().create(AuthApiService::class.java)

    /**
     * ScheduleApiService实例
     */
    val scheduleApiService: ScheduleApiService
        get() = getRetrofit().create(ScheduleApiService::class.java)

    /**
     * PrivacyApiService实例
     */
    val privacyApiService: PrivacyApiService
        get() = getRetrofit().create(PrivacyApiService::class.java)

    /**
     * AvatarApiService实例
     */
    val avatarApiService: AvatarApiService
        get() = getRetrofit().create(AvatarApiService::class.java)

    /**
     * AIChatApiService实例
     */
    val aiChatApiService: AIChatApiService
        get() = getRetrofit().create(AIChatApiService::class.java)

    /**
     * GroupApiService实例
     */
    val groupApiService: GroupApiService
        get() = getRetrofit().create(GroupApiService::class.java)
}