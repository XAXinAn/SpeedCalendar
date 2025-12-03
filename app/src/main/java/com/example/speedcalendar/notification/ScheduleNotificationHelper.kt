package com.example.speedcalendar.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.speedcalendar.MainActivity
import com.example.speedcalendar.R
import com.example.speedcalendar.data.model.Schedule

/**
 * 通知帮助类
 * 用于创建和管理日程通知 - 使用分组通知实现堆叠效果
 */
object ScheduleNotificationHelper {

    private const val TAG = "ScheduleNotification"

    // 通知渠道 ID
    const val CHANNEL_ID_DAILY_SCHEDULE = "daily_schedule_channel_v4"
    private const val CHANNEL_NAME = "今日日程"
    private const val CHANNEL_DESCRIPTION = "在锁屏显示今天的日程安排"

    // 通知组 ID
    private const val GROUP_KEY_SCHEDULES = "com.example.speedcalendar.SCHEDULES"
    
    // 通知 ID
    const val NOTIFICATION_ID_SUMMARY = 2000  // 摘要通知 ID
    private const val NOTIFICATION_ID_BASE = 2001  // 单个日程通知的起始 ID
    
    // 当前显示的通知数量
    private var currentNotificationCount = 0

    /**
     * 创建通知渠道（Android 8.0+）
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_DAILY_SCHEDULE,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(false)
                enableVibration(false)
                setSound(null, null)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 构建单个日程通知
     */
    private fun buildScheduleNotification(
        context: Context,
        schedule: Schedule,
        notificationId: Int,
        isOngoing: Boolean = true
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeStr = if (schedule.isAllDay || schedule.startTime.isNullOrEmpty()) "全天" else schedule.startTime
        val title = "$timeStr  ${schedule.title}"
        val content = if (!schedule.location.isNullOrEmpty()) "📍 ${schedule.location}" else "点击查看详情"
        
        return NotificationCompat.Builder(context, CHANNEL_ID_DAILY_SCHEDULE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY_SCHEDULES)
            .setOngoing(isOngoing)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setShowWhen(false)
            .setAutoCancel(false)
            .build()
    }

    /**
     * 构建摘要通知（分组的父通知）
     */
    private fun buildSummaryNotification(
        context: Context,
        schedules: List<Schedule>
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_SUMMARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val count = schedules.size
        val title = "📅 今日待办 ($count)"
        
        // 构建展开后的内容
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
        
        schedules.forEach { schedule ->
            val timeStr = if (schedule.isAllDay || schedule.startTime.isNullOrEmpty()) "全天" else schedule.startTime
            inboxStyle.addLine("$timeStr  ${schedule.title}")
        }

        // 第一条日程的简要信息
        val firstSchedule = schedules.first()
        val firstTimeStr = if (firstSchedule.isAllDay || firstSchedule.startTime.isNullOrEmpty()) "全天" else firstSchedule.startTime
        val contentText = if (count == 1) {
            "$firstTimeStr ${firstSchedule.title}"
        } else {
            "$firstTimeStr ${firstSchedule.title} 等${count}项"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID_DAILY_SCHEDULE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY_SCHEDULES)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setShowWhen(false)
            .setStyle(inboxStyle)
            .setNumber(count)
            .setSubText("$count 项待办")
            .build()
    }

    /**
     * 构建空日程通知（无待办事项时显示）
     */
    private fun buildEmptyNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_SUMMARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_DAILY_SCHEDULE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📅 今日日程")
            .setContentText("暂无待办日程")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setShowWhen(false)
            .build()
    }

    /**
     * 为前台服务构建初始通知
     */
    fun buildDailyScheduleNotification(
        context: Context,
        schedules: List<Schedule>
    ): Notification {
        return if (schedules.isEmpty()) {
            buildEmptyNotification(context)
        } else {
            buildSummaryNotification(context, schedules)
        }
    }

    /**
     * 显示/更新今日日程通知
     * 使用分组通知：每个日程一条通知 + 一条摘要通知
     */
    fun showDailyScheduleNotification(context: Context, schedules: List<Schedule>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        Log.d(TAG, "========== 更新通知开始 ==========")
        Log.d(TAG, "日程数量: ${schedules.size}")
        schedules.forEachIndexed { index, schedule -> 
            Log.d(TAG, "日程[$index]: ${schedule.startTime ?: "全天"} - ${schedule.title}")
        }
        
        // 先清除所有旧通知
        cancelAllScheduleNotifications(context, notificationManager)
        
        if (schedules.isEmpty()) {
            // 无日程时显示空状态通知
            val notification = buildEmptyNotification(context)
            notificationManager.notify(NOTIFICATION_ID_SUMMARY, notification)
            currentNotificationCount = 0
            Log.d(TAG, "显示空日程通知")
        } else {
            // 按时间排序
            val sortedSchedules = schedules.sortedWith(compareBy(
                { it.isAllDay },  // 全天日程排后面
                { it.startTime ?: "99:99" }  // 按开始时间排序
            ))
            
            // 发送每个日程的单独通知（子通知）
            sortedSchedules.forEachIndexed { index, schedule ->
                val notificationId = NOTIFICATION_ID_BASE + index
                val notification = buildScheduleNotification(context, schedule, notificationId)
                notificationManager.notify(notificationId, notification)
                Log.d(TAG, "发送子通知: id=$notificationId, title=${schedule.title}")
            }
            
            // 发送摘要通知（父通知，用于分组显示）
            val summaryNotification = buildSummaryNotification(context, sortedSchedules)
            notificationManager.notify(NOTIFICATION_ID_SUMMARY, summaryNotification)
            
            currentNotificationCount = sortedSchedules.size
            Log.d(TAG, "发送摘要通知，共 ${sortedSchedules.size} 条子通知")
        }
    }

    /**
     * 取消所有日程通知
     */
    private fun cancelAllScheduleNotifications(context: Context, notificationManager: NotificationManager? = null) {
        val nm = notificationManager ?: context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 取消摘要通知
        nm.cancel(NOTIFICATION_ID_SUMMARY)
        
        // 取消所有子通知（最多支持50条）
        val maxCount = maxOf(currentNotificationCount, 50)
        for (i in 0 until maxCount) {
            nm.cancel(NOTIFICATION_ID_BASE + i)
        }
        
        Log.d(TAG, "已清除所有旧通知")
    }

    /**
     * 取消今日日程通知（公开方法）
     */
    fun cancelDailyScheduleNotification(context: Context) {
        cancelAllScheduleNotifications(context)
        currentNotificationCount = 0
    }
}
