package com.example.data.local

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val nativeName: String) {
    ENGLISH("en", "EN"),
    SPANISH("es", "ES")
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }
val LocalLanguageManager = compositionLocalOf<LanguageManager?> { null }

class LanguageManager(context: Context) {
    private val prefs = context.getSharedPreferences("wearwise_lang_prefs", Context.MODE_PRIVATE)
    private val _currentLanguage = MutableStateFlow(
        if (prefs.getString("lang", "en") == "es") AppLanguage.SPANISH else AppLanguage.ENGLISH
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun toggleLanguage() {
        val next = if (_currentLanguage.value == AppLanguage.ENGLISH) AppLanguage.SPANISH else AppLanguage.ENGLISH
        _currentLanguage.value = next
        prefs.edit().putString("lang", next.code).apply()
    }

    fun getString(key: String, lang: AppLanguage = _currentLanguage.value): String {
        return when (key) {
            "app_title" -> "WEARWISE"
            "app_subtitle" -> if (lang == AppLanguage.SPANISH) "IA ALTA COSTURA" else "AI COUTURE"
            else -> key
        }
    }
}
