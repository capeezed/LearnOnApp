package com.learnon.app.instructor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LearnOnBackground = Color(0xFF080C16)
private val LearnOnSurface = Color(0xFF0F1222)
private val LearnOnBorder = Color(0xFF363A4F)
private val LearnOnText = Color(0xFFECEEF9)
private val LearnOnTextMuted = Color(0xFFB4B4C3)
private val LearnOnPurple = Color(0xFF4937A6)

private val LightColors = lightColorScheme(
    primary = LearnOnPurple,
    secondary = LearnOnPurple,
    tertiary = LearnOnPurple,
    background = LearnOnBackground,
    surface = LearnOnSurface,
    surfaceVariant = LearnOnBorder,
    onPrimary = LearnOnText,
    onSecondary = LearnOnText,
    onTertiary = LearnOnText,
    onBackground = LearnOnText,
    onSurface = LearnOnText,
    onSurfaceVariant = LearnOnTextMuted,
)

private val DarkColors = darkColorScheme(
    primary = LearnOnPurple,
    secondary = LearnOnPurple,
    tertiary = LearnOnPurple,
    background = LearnOnBackground,
    surface = LearnOnSurface,
    surfaceVariant = LearnOnBorder,
    onPrimary = LearnOnText,
    onSecondary = LearnOnText,
    onTertiary = LearnOnText,
    onBackground = LearnOnText,
    onSurface = LearnOnText,
    onSurfaceVariant = LearnOnTextMuted,
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
