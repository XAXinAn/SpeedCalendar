package com.example.speedcalendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Dark theme colors, ensuring no purple tints
private val DarkColorScheme = darkColorScheme(
    primary = Blue80, // Using the new lighter blue for dark mode
    secondary = BlueGrey80,
    tertiary = LightBlue80,
    background = Color(0xFF121212), // A common dark background
    surface = Color(0xFF1C1B1F),      // A slightly lighter surface
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
    background = Background,       // Use the new background color
    surface = Color.White,          // Use white for surfaces like cards
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color is disabled to ensure consistent branding
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
