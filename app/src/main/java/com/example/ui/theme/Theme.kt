package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WinDarkPrimary,
    onPrimary = Color(0xFF002050),
    primaryContainer = Color(0xFF003888),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = WinDarkSecondary,
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = WinDarkTertiary,
    onTertiary = Color(0xFF3E2E00),
    background = WinDarkBackground,
    onBackground = Color(0xFFEAF0F8),
    surface = WinDarkSurface,
    onSurface = Color(0xFFEAF0F8),
    surfaceVariant = WinDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC5D1E0)
)

private val LightColorScheme = lightColorScheme(
    primary = WinNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B47),
    secondary = WinTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8E8),
    onSecondaryContainer = Color(0xFF002020),
    tertiary = WinGoldAccent,
    onTertiary = Color.White,
    background = Color(0xFFF3F4F8),
    onBackground = Color(0xFF141B26),
    surface = Color.White,
    onSurface = Color(0xFF141B26),
    surfaceVariant = Color(0xFFE5E8F0),
    onSurfaceVariant = Color(0xFF3D4654)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
