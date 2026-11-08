package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.remote.GeminiClient
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
            Result.success(GeminiClient.verifyConnection(key))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = GeminiClient.describeConnectionFailure(e, key, "connection test")
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
