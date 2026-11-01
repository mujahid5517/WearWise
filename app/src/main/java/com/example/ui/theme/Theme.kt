package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
    secondary = Color(0xFF9A5700),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF047857),
    onTertiary = Color.White,
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF171923),
    surface = Color.White,
    onSurface = Color(0xFF171923),
    surfaceVariant = Color(0xFFF0F1F7),
    onSurfaceVariant = Color(0xFF687085),
    outline = Color(0xFFE5E7EF),
    outlineVariant = Color(0xFFD8DCE8),
    error = Color(0xFFCF304B),
    onError = Color.White
)

@Composable
fun WearWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    SideEffect { WearWisePalette.update(darkTheme) }
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
