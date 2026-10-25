package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WearWiseColorScheme = darkColorScheme(
    primary = RoyalElectricViolet,
    onPrimary = Color.White,
    primaryContainer = RoyalVioletDark,
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = CoutureAmber,
    onSecondary = ObsidianBlack,
    secondaryContainer = Color(0xFF452B05),
    onSecondaryContainer = CoutureAmberLight,
    tertiary = CoutureEmerald,
    onTertiary = ObsidianBlack,
    background = DarkNavyBg,
    onBackground = TextPrimary,
    surface = CardElevated,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1B2032),
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = Color(0xFF2E364F),
    error = CoutureRose,
    onError = Color.White
)

@Composable
fun WearWiseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WearWiseColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WearWiseTheme(content = content)
}
