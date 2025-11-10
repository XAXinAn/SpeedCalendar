package com.example.speedcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.speedcalendar.ui.MainScreen
import com.example.speedcalendar.ui.theme.SpeedCalendarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var keepSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 安装启动屏
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 2. 设置启动屏的持续显示条件
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        // 3. 启动一个协程，在3秒后改变条件
        lifecycleScope.launch {
            delay(3000) // 等待3秒
            keepSplashOnScreen = false // 改变条件，让启动屏消失
        }

        enableEdgeToEdge()
        setContent {
            SpeedCalendarTheme {
                MainScreen()
            }
        }
    }
}