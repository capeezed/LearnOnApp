package com.learnon.app.instructor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF0F766E),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8ECF2),
    onPrimary = Color.White,
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF5B6472),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    secondary = Color(0xFF5EEAD4),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF0B0F17),
    surface = Color(0xFF121826),
    surfaceVariant = Color(0xFF1F2937),
    onPrimary = Color(0xFF07111F),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFB8C0CC),
)

@Composable
fun LearnOnInstructorTheme(
    colorScheme: ColorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
