package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WearWiseDarkColorScheme = darkColorScheme(
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

private val WearWiseLightColorScheme = lightColorScheme(
    primary = RoyalElectricViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = RoyalVioletDark,
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF047857),
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2FF),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = CoutureRose,
    onError = Color.White
)

@Composable
fun WearWiseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) WearWiseDarkColorScheme else WearWiseLightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WearWiseTheme(darkTheme = darkTheme, content = content)
}
