package com.example.fit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Accent colors (not part of the Material scheme)
val SuccessGreen = Color(0xFF34C759)
val SkipBlue = Color(0xFF5AC8FA)
val RpePurple = Color(0xFF9C27B0)
val EquipmentGreen = Color(0xFF2E7D32)

// Standalone refs used in composables
val TextSecondary = Color(0xFF8E8E93)

/**
 * Sizing scale — all spacing/padding/heights derive from this base unit.
 * Change `base` to uniformly scale the entire UI.
 */
data class FitSizing(
    val base: Dp = 4.dp,                // fundamental unit
    val xxs: Dp = base,                 // 4dp
    val xs: Dp = base * 2,              // 8dp
    val sm: Dp = base * 3,              // 12dp
    val md: Dp = base * 4,              // 16dp
    val lg: Dp = base * 6,              // 24dp
    val xl: Dp = base * 8,              // 32dp
    val cardCorner: Dp = base * 3,      // 12dp
    val chipCorner: Dp = base,          // 4dp
    val buttonCorner: Dp = base * 3,    // 12dp
    val inputHeight: Dp = base * 12,    // 48dp
    val cardPadding: Dp = base * 3,     // 12dp
    val cardGap: Dp = base * 2,         // 8dp
)

val LocalFitSizing = compositionLocalOf { FitSizing() }

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
