package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Existing UI tokens switch centrally so legacy screens and dialogs follow the selected theme. */
internal object WearWisePalette {
    var isLight by mutableStateOf(false)
        private set

    fun update(darkTheme: Boolean) {
        isLight = !darkTheme
    }
}

// WearWise Haute Couture dark palette and its premium light counterpart.
val ObsidianBlack = Color(0xFF090B10)
val DarkNavyBg: Color get() = if (WearWisePalette.isLight) Color(0xFFF7F8FC) else Color(0xFF0C0F17)
val CardElevated: Color get() = if (WearWisePalette.isLight) Color.White else Color(0xFF131724)
val CardBorder: Color get() = if (WearWisePalette.isLight) Color(0xFFE5E7EF) else Color(0xFF22283A)
val CardSelected: Color get() = if (WearWisePalette.isLight) Color(0xFFECEBFF) else Color(0xFF1F253C)
val RoyalElectricViolet = Color(0xFF5E5BF7)
val RoyalElectricVioletBright = Color(0xFF706EFA)
val RoyalVioletDark = Color(0xFF4338CA)
val VioletGlow = Color(0x336366F1)

val CoutureAmber: Color get() = if (WearWisePalette.isLight) Color(0xFF9A5700) else Color(0xFFF59E0B)
val CoutureAmberLight: Color get() = if (WearWisePalette.isLight) Color(0xFFB45309) else Color(0xFFFBBF24)
val CoutureEmerald: Color get() = if (WearWisePalette.isLight) Color(0xFF047857) else Color(0xFF10B981)
val CoutureRose: Color get() = if (WearWisePalette.isLight) Color(0xFFCF304B) else Color(0xFFF43F5E)

val TextPrimary: Color get() = if (WearWisePalette.isLight) Color(0xFF171923) else Color(0xFFF8FAFC)
val TextSecondary: Color get() = if (WearWisePalette.isLight) Color(0xFF687085) else Color(0xFF94A3B8)
val TextMuted: Color get() = if (WearWisePalette.isLight) Color(0xFF7B8498) else Color(0xFF64748B)

val ChipActiveBg: Color get() = if (WearWisePalette.isLight) Color(0xFFEAE9FF) else Color(0xFF2E2A72)
val ChipActiveBorder = Color(0xFF6366F1)
val ChipInactiveBg: Color get() = if (WearWisePalette.isLight) Color(0xFFF2F3F8) else Color(0xFF151926)
val ChipInactiveBorder: Color get() = if (WearWisePalette.isLight) Color(0xFFE5E7EF) else Color(0xFF252B3E)
val WarningContainer: Color get() = if (WearWisePalette.isLight) Color(0xFFFFF3D6) else Color(0xFF1A1200)
val InfoContainer: Color get() = if (WearWisePalette.isLight) Color(0xFFEFEEFF) else Color(0xFF0D0D2B)
val SuccessContainer: Color get() = if (WearWisePalette.isLight) Color(0xFFE8F7F0) else Color(0xFF071A12)
val DangerContainer: Color get() = if (WearWisePalette.isLight) Color(0xFFFFEEF0) else Color(0xFF261217)
val DangerBorder: Color get() = if (WearWisePalette.isLight) Color(0xFFF2C6CE) else Color(0xFF5C1B28)
