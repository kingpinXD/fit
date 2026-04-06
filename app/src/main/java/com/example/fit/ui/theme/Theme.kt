package com.example.fit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Accent colors (not part of the Material scheme)
val SuccessGreen = Color(0xFF34C759)
val SkipBlue = Color(0xFF5AC8FA)

// Standalone refs used in composables
val TextSecondary = Color(0xFF8E8E93)

private val BlackSilverColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2C2C2E),
    secondary = Color(0xFF8E8E93),
    surface = Color(0xFF1C1C1E),
    background = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF38383A),
    error = Color(0xFFFF453A),
)

@Composable
fun FitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlackSilverColorScheme,
        typography = Typography(),
        content = content
    )
}
