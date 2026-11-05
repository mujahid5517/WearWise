package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import com.example.BuildConfig
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateContentRequest
import com.example.data.remote.GeminiPart
import com.example.data.remote.generateTextContent
import com.example.data.remote.GeminiErrorEnvelope
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiImageConfig
import retrofit2.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _customApiKeyFlow = MutableStateFlow(readCustomApiKey())
    val customApiKeyFlow: StateFlow<String?> = _customApiKeyFlow.asStateFlow()

    fun getCustomApiKey(): String? {
        return readCustomApiKey()
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
        prefs.edit().putString(KEY_GEMINI_API_KEY, encryptKey(cleanKey)).apply()
        _customApiKeyFlow.value = cleanKey
    }

    fun clearCustomApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
        _customApiKeyFlow.value = null
    }

    private fun readCustomApiKey(): String? {
        val stored = prefs.getString(KEY_GEMINI_API_KEY, null)?.takeIf { it.isNotBlank() } ?: return null
        return if (stored.startsWith(ENCRYPTED_PREFIX)) {
            runCatching { decryptKey(stored.removePrefix(ENCRYPTED_PREFIX)) }.getOrNull()
        } else {
            // Migrate keys saved by previous app versions from private preferences to encrypted storage.
            runCatching { encryptKey(stored) }.getOrNull()?.let { encrypted ->
                prefs.edit().putString(KEY_GEMINI_API_KEY, encrypted).apply()
                stored
            }
        }
    }

    private fun encryptKey(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted
        return ENCRYPTED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decryptKey(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > GCM_IV_BYTES) { "Invalid encrypted API key." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_BITS, payload.copyOfRange(0, GCM_IV_BYTES))
        )
        return cipher.doFinal(payload.copyOfRange(GCM_IV_BYTES, payload.size)).toString(Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    suspend fun testApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("API key cannot be empty"))
        }
        try {
            val startTime = System.currentTimeMillis()
            val textRequest = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = "Respond with 'OK' if connection is active.")))
                )
            )

            // Test text generation connection first
            val response = GeminiClient.apiService.generateTextContent(key, textRequest)
            val latency = System.currentTimeMillis() - startTime
            val textReply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!textReply.isNullOrBlank()) {
                return@withContext Result.success("Connection successful: Gemini AI service connected (${latency} ms).")
            }

            // Fallback image test if text response was empty
            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = "Generate a simple abstract blue circle on a white background."))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("IMAGE"),
                    imageConfig = GeminiImageConfig(aspectRatio = "1:1", imageSize = "512")
                )
            )
            val imgResponse = GeminiClient.apiService.generateContent(GeminiClient.IMAGE_MODEL, key, request)
            val imagePart = imgResponse.candidates.orEmpty()
                .asSequence()
                .flatMap { it.content?.parts.orEmpty().asSequence() }
                .firstOrNull { !it.inlineData?.data.isNullOrBlank() }
                ?.inlineData
            val imageBytes = imagePart?.data?.let { runCatching { Base64.decode(it, Base64.DEFAULT) }.getOrNull() }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (imageBytes == null || imageBytes.isEmpty() || imagePart?.mimeType?.startsWith("image/") != true) {
                Result.failure(IllegalStateException("AI model returned no content. Check API key access and try again."))
            } else {
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    Result.failure(IllegalStateException("AI model returned data that is not decodable."))
                } else {
                    Result.success("Connection successful: Gemini AI generated image (${latency} ms).")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = if (e is HttpException) {
                val detail = runCatching {
                    e.response()?.errorBody()?.string()?.let {
                        GeminiClient.moshi.adapter(GeminiErrorEnvelope::class.java).fromJson(it)
                    }?.error
                }.getOrNull()
                val apiMessage = detail?.message.orEmpty().replace(key, "[redacted]").take(200)
                when (e.code()) {
                    401 -> "Invalid API key. Check the key and try again."
                    403 -> if (apiMessage.contains("billing", true) || apiMessage.contains("payment", true)) {
                        "Billing required: enable billing for the Google project used by this key."
                    } else "Permission denied for image generation. Check Gemini API and model permissions. $apiMessage"
                    404 -> "Image-generation model or endpoint unavailable: ${GeminiClient.IMAGE_MODEL}. Check the model ID and API endpoint."
                    429 -> if (detail?.status.equals("RESOURCE_EXHAUSTED", true) || apiMessage.contains("quota", true)) {
                        "Image-generation quota reached. Try again after the quota resets. $apiMessage"
                    } else "Image-generation rate limit reached. Wait briefly and try again."
                    400 -> "Gemini rejected the image test request. Check the request format. $apiMessage"
                    in 500..599 -> "Gemini image generation is temporarily unavailable (HTTP ${e.code()})."
                    else -> "Gemini image-generation check failed (HTTP ${e.code()}). $apiMessage"
                }
            } else if (e is java.io.IOException) {
                "Unable to connect to the AI service. Check your internet connection and try again."
            } else (e.localizedMessage ?: "Gemini image-generation check failed.").replace(key, "[redacted]")
            Result.failure(IllegalStateException(message))
        }
    }

    companion object {
        private const val PREFS_NAME = "wearwise_secure_prefs"
        private const val KEY_GEMINI_API_KEY = "custom_gemini_api_key"
        private const val KEY_ALIAS = "wearwise_gemini_api_key"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ENCRYPTED_PREFIX = "enc:v1:"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
    }
}
