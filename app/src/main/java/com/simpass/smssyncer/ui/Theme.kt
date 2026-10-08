package com.simpass.smssyncer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF176D58),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF2E8),
    onPrimaryContainer = Color(0xFF124D3E),
    secondary = Color(0xFF53685F),
    secondaryContainer = Color(0xFFEAF0EC),
    background = Color(0xFFF7F9F8),
    surface = Color(0xFFF7F9F8),
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    onSurface = Color(0xFF1B2922),
    onSurfaceVariant = Color(0xFF637269),
    outlineVariant = Color(0xFFDDE5DF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF93D8B8),
    onPrimary = Color(0xFF003827),
    primaryContainer = Color(0xFF154F3C),
    onPrimaryContainer = Color(0xFFB4EED1),
    background = Color(0xFF101813),
    surface = Color(0xFF101813),
    surfaceContainer = Color(0xFF1C2820),
    surfaceContainerLow = Color(0xFF1C2820),
    onSurface = Color(0xFFE0EAE2),
    onSurfaceVariant = Color(0xFFA6B7A9),
    outlineVariant = Color(0xFF3C4D41),
)

@Composable
fun SmsSyncerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
