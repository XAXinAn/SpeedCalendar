package com.example.speedcalendar.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.local.UserPreferences
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 今日日程前台服务
 * 在通知栏常驻显示今日日程
 */
class DailyScheduleService : Service() {

    companion object {
        private const val TAG = "DailyScheduleService"
        private const val ACTION_REFRESH = "com.example.speedcalendar.ACTION_REFRESH_NOTIFICATION"
        
        // 用于从外部更新通知
        private var instance: DailyScheduleService? = null
        
        /**
         * 刷新日程通知（供外部调用）
         * 如果服务正在运行，直接刷新；否则重新启动服务
         */
        fun refreshNotification(context: Context? = null) {
            Log.d(TAG, "refreshNotification called, instance=$instance")
            
            if (instance != null) {
                // 服务正在运行，直接刷新
                Log.d(TAG, "服务运行中，直接刷新")
                instance?.loadTodaySchedules()
            } else if (context != null) {
                // 服务未运行，重新启动
                Log.d(TAG, "服务未运行，重新启动服务")
                val intent = Intent(context, DailyScheduleService::class.java).apply {
                    action = ACTION_REFRESH
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                Log.w(TAG, "无法刷新通知：服务未运行且没有提供Context")
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var userPreferences: UserPreferences

    override fun onCreate() {
        super.onCreate()
        instance = this
        userPreferences = UserPreferences.getInstance(this)
        ScheduleNotificationHelper.createNotificationChannel(this)
        Log.d(TAG, "服务创建, instance已设置")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "服务启动, action=${intent?.action}")
        
        // 确保 instance 已设置
        instance = this
        
        // 启动前台服务，先显示空日程通知
        val notification = ScheduleNotificationHelper.buildDailyScheduleNotification(this, emptyList())
        startForeground(ScheduleNotificationHelper.NOTIFICATION_ID_SUMMARY, notification)
        
        // 加载今日日程
        loadTodaySchedules()
        
        return START_STICKY  // 服务被杀死后自动重启
    }

    /**
     * 加载今日日程并更新通知
     */
    fun loadTodaySchedules() {
        serviceScope.launch {
            try {
                val token = userPreferences.getAccessToken()
                if (token != null) {
                    val today = LocalDate.now()
                    val currentTime = LocalTime.now()
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val currentTimeStr = currentTime.format(timeFormatter)
                    
                    Log.d(TAG, "加载日程: ${today.year}-${today.monthValue}, 当前时间: $currentTimeStr")
                    
                    val response = RetrofitClient.scheduleApiService.getSchedules(
                        "Bearer $token",
                        today.year,
                        today.monthValue
                    )
                    
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.code == 200) {
                            val allSchedules = apiResponse.data ?: emptyList()
                            
                            Log.d(TAG, "API返回日程总数: ${allSchedules.size}")
                            
                            // 过滤出今天的日程，且只显示当前时间之后的日程
                            val upcomingSchedules = allSchedules.filter { schedule ->
                                // 首先检查是否是今天的日程
                                val isToday = schedule.scheduleDate == today.toString()
                                Log.d(TAG, "日程[${schedule.title}]: date=${schedule.scheduleDate}, today=${today}, isToday=$isToday")
                                
                                if (!isToday) {
                                    return@filter false
                                }
                                
                                // 全天日程始终显示
                                if (schedule.isAllDay || schedule.startTime.isNullOrEmpty()) {
                                    Log.d(TAG, "  -> 全天日程，显示")
                                    return@filter true
                                }
                                
                                // 比较时间：只显示开始时间在当前时间之后的日程
                                try {
                                    val scheduleTime = LocalTime.parse(schedule.startTime, timeFormatter)
                                    val shouldShow = scheduleTime >= currentTime
                                    Log.d(TAG, "  -> 时间比较: ${schedule.startTime} vs $currentTimeStr, 显示=$shouldShow")
                                    shouldShow
                                } catch (e: Exception) {
                                    Log.w(TAG, "  -> 解析时间失败: ${schedule.startTime}, ${e.message}, 默认显示")
                                    true  // 解析失败的日程默认显示
                                }
                            }
                            
                            Log.d(TAG, "过滤后日程数量: ${upcomingSchedules.size}")
                            upcomingSchedules.forEach {
                                Log.d(TAG, "  显示: ${it.startTime ?: "全天"} - ${it.title}")
                            }
                            
                            // 更新通知
                            withContext(Dispatchers.Main) {
                                ScheduleNotificationHelper.showDailyScheduleNotification(
                                    this@DailyScheduleService, 
                                    upcomingSchedules
                                )
                            }
                        }
                    } else {
                        Log.e(TAG, "请求失败: ${response.code()}")
                    }
                } else {
                    Log.d(TAG, "用户未登录，显示空日程")
                    withContext(Dispatchers.Main) {
                        ScheduleNotificationHelper.showDailyScheduleNotification(
                            this@DailyScheduleService, 
                            emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载日程失败: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        Log.d(TAG, "服务销毁")
    }
}
