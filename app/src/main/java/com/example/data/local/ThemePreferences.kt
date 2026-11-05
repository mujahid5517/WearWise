package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark");

    fun resolvesToDark(systemInDark: Boolean = true): Boolean {
        return when (this) {
            SYSTEM -> systemInDark
            LIGHT -> false
            DARK -> true
        }
    }
}

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("wearwise_theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching {
            AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name)
        }.getOrDefault(AppThemeMode.DARK)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }
}
