package com.example.presentation.common

import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(val code: String, val nativeName: String) {
    EN("en", "English"),
    ES("es", "Español"),
    FR("fr", "Français")
}

class LanguageManager {
    var currentLanguage: AppLanguage = AppLanguage.EN
        private set

    fun toggleLanguage() {
        currentLanguage = when (currentLanguage) {
            AppLanguage.EN -> AppLanguage.ES
            AppLanguage.ES -> AppLanguage.FR
            AppLanguage.FR -> AppLanguage.EN
        }
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.EN }
val LocalLanguageManager = compositionLocalOf { LanguageManager() }
