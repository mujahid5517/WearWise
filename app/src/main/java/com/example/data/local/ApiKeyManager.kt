package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateContentRequest
import com.example.data.remote.GeminiPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ApiKeyManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _customApiKeyFlow = MutableStateFlow(getCustomApiKey())
    val customApiKeyFlow: StateFlow<String?> = _customApiKeyFlow.asStateFlow()

    fun getCustomApiKey(): String? {
        val key = prefs.getString(KEY_GEMINI_API_KEY, null)
        return if (!key.isNullOrBlank()) key.trim() else null
    }

    fun hasCustomKey(): Boolean {
        return !getCustomApiKey().isNullOrEmpty()
    }

    fun getEffectiveApiKey(): String {
        val custom = getCustomApiKey()
        if (!custom.isNullOrEmpty()) {
            return custom
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        return if (buildKey.isNotEmpty() && !buildKey.contains("MY_GEMINI_API_KEY")) buildKey else ""
    }

    fun saveCustomApiKey(key: String) {
        val cleanKey = key.trim()
        prefs.edit().putString(KEY_GEMINI_API_KEY, cleanKey).apply()
        _customApiKeyFlow.value = cleanKey
    }

    fun clearCustomApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
        _customApiKeyFlow.value = null
    }

    suspend fun testApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("API key cannot be empty"))
        }
        try {
            val startTime = System.currentTimeMillis()
            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = "Hello! Reply with OK."))
                    )
                )
            )
            val response = GeminiClient.apiService.generateTextContent(key, request)
            val latency = System.currentTimeMillis() - startTime
            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!replyText.isNullOrEmpty()) {
                Result.success("Connected successfully ($latency ms)")
            } else {
                Result.success("Connected ($latency ms)")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val PREFS_NAME = "wearwise_secure_prefs"
        private const val KEY_GEMINI_API_KEY = "custom_gemini_api_key"
    }
}
