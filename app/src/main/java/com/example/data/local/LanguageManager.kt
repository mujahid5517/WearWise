package com.example.data.local

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val nativeName: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français")
}

class LanguageManager(private val context: Context) {
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun toggleLanguage() {
        _currentLanguage.value = when (_currentLanguage.value) {
            AppLanguage.ENGLISH -> AppLanguage.SPANISH
            AppLanguage.SPANISH -> AppLanguage.FRENCH
            AppLanguage.FRENCH -> AppLanguage.ENGLISH
        }
    }

    fun getString(key: String, lang: AppLanguage): String {
        return when (key) {
            "app_title" -> "WEARWISE"
            "app_subtitle" -> "AI COUTURE"
            else -> key
        }
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }
val LocalLanguageManager = compositionLocalOf<LanguageManager?> { null }
