package com.example.speedcalendar

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.speedcalendar.features.ai.FloatingWindowService
import com.example.speedcalendar.ui.MainScreen
import com.example.speedcalendar.ui.theme.SpeedCalendarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var keepSplashOnScreen = true
    
    // MediaProjection 权限请求
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        lifecycleScope.launch {
            delay(3000)
            keepSplashOnScreen = false
        }
        
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
                    onRequestScreenCapture = { requestScreenCapturePermission() }
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
}