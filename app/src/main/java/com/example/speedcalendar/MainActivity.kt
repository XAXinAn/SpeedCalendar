package com.example.speedcalendar

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.speedcalendar.data.local.UserPreferences
import com.example.speedcalendar.features.ai.FloatingWindowService
import com.example.speedcalendar.notification.DailyScheduleService
import com.example.speedcalendar.ui.MainScreen
import com.example.speedcalendar.ui.theme.SpeedCalendarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var keepSplashOnScreen = true
    
    // MediaProjection 权限请求
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>
    
    // 通知权限请求 (Android 13+)
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        lifecycleScope.launch {
            delay(3000)
            keepSplashOnScreen = false
        }
        
        // 初始化通知权限请求
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d(TAG, "通知权限结果: $isGranted")
            if (isGranted) {
                // 权限获取成功，启动日程通知服务
                startDailyScheduleService()
            } else {
                Toast.makeText(this, "需要通知权限才能在锁屏显示日程", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 请求通知权限并启动服务
        requestNotificationPermissionAndStartService()
        
        // 初始化截屏权限请求
        screenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // 权限获取成功，启动悬浮窗服务并传递权限数据
                val serviceIntent = Intent(this, FloatingWindowService::class.java).apply {
                    putExtra(FloatingWindowService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(FloatingWindowService.EXTRA_DATA, result.data)
                }
                startService(serviceIntent)
            } else {
                Toast.makeText(this, "需要截屏权限才能使用悬浮窗功能", Toast.LENGTH_SHORT).show()
            }
        }

        enableEdgeToEdge()
        setContent {
            SpeedCalendarTheme {
                MainScreen(
                    onRequestScreenCapture = { requestScreenCapturePermission() },
                    onLoginSuccess = { requestNotificationPermissionAndStartService() }
                )
            }
        }
    }
    
    /**
     * 请求截屏权限
     */
    private fun requestScreenCapturePermission() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
    
    /**
     * 如果用户已登录，启动日程通知服务
     */
    private fun startDailyScheduleServiceIfLoggedIn() {
        val userPreferences = UserPreferences.getInstance(this)
        if (userPreferences.getAccessToken() != null) {
            startDailyScheduleService()
        }
    }
    
    /**
     * 请求通知权限并启动服务
     */
    private fun requestNotificationPermissionAndStartService() {
        val userPreferences = UserPreferences.getInstance(this)
        // 只有登录用户才需要启动通知服务
        if (userPreferences.getAccessToken() == null) {
            Log.d(TAG, "用户未登录，跳过通知服务")
            return
        }
        
        // Android 13+ 需要动态请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, 
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 已有权限，直接启动服务
                    Log.d(TAG, "已有通知权限，启动服务")
                    startDailyScheduleService()
                }
                else -> {
                    // 请求权限
                    Log.d(TAG, "请求通知权限")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 及以下版本不需要动态请求
            Log.d(TAG, "Android 12及以下，直接启动服务")
            startDailyScheduleService()
        }
    }
    
    /**
     * 启动日程通知服务
     */
    private fun startDailyScheduleService() {
        Log.d(TAG, "启动 DailyScheduleService")
        val serviceIntent = Intent(this, DailyScheduleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}