package com.example.speedcalendar

import android.app.Application
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.notification.ScheduleNotificationHelper

/**
 * 应用程序入口
 * 用于初始化全局组件
 */
class SpeedCalendarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 初始化RetrofitClient（用于自动Token刷新）
        RetrofitClient.init(this)
        
        // 创建日程通知渠道
        ScheduleNotificationHelper.createNotificationChannel(this)
    }
}
