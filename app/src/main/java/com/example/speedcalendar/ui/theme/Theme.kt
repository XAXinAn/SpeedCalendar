package com.example.speedcalendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark theme colors, ensuring no purple tints
private val DarkColorScheme = darkColorScheme(
    primary = Blue80, // Using the new lighter blue for dark mode
    secondary = BlueGrey80,
    tertiary = LightBlue80,
    background = Color(0xFF1C1B1F), // Dark grey background
    surface = Color(0xFF1C1B1F),      // Dark grey surface
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

// Light theme colors, ensuring no purple tints
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue, // Using the specified #0099FD blue
    secondary = BlueGrey40,
    tertiary = LightBlue40,
    background = Color.White,       // Pure white background
    surface = Color.White,          // Pure white surface
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun SpeedCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color is disabled to ensure consistent branding
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
