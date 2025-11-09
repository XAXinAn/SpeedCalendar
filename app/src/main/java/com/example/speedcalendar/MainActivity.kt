package com.example.speedcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.speedcalendar.ui.MainScreen
import com.example.speedcalendar.ui.theme.SpeedCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpeedCalendarTheme {
                MainScreen()
            }
        }
    }
}