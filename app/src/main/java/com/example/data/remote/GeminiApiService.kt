package com.example.data.remote

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.IOException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val responseModalities: List<String>? = null,
    val imageConfig: GeminiImageConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageConfig(
    val aspectRatio: String = "3:4",
    val imageSize: String = "1K"
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate>? = null,
    val promptFeedback: GeminiPromptFeedback? = null,
    val error: GeminiErrorDetail? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPromptFeedback(
    val blockReason: String? = null,
    val blockReasonMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorEnvelope(
    val error: GeminiErrorDetail? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorDetail(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse

    @POST("v1beta/models/gemini-3.6-flash:generateContent")
    suspend fun generateTextContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse

    @POST("v1beta/models/gemini-3.1-flash-image:generateContent")
    suspend fun generateImageContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse
}

private class RedactingHttpLogger : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val started = System.currentTimeMillis()
        return try {
            val response = chain.proceed(request)
            val elapsed = System.currentTimeMillis() - started
            Log.d(
                "WearWiseGemini",
                "HTTP ${response.code} ${redactUrl(request.url)} (${elapsed}ms)"
            )
            response
        } catch (e: IOException) {
            Log.w("WearWiseGemini", "HTTP failed ${redactUrl(request.url)}")
            throw e
        }
    }

    private fun redactUrl(url: HttpUrl): String {
        return url.newBuilder()
            .setQueryParameter("key", "REDACTED")
            .build()
            .toString()
    }
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    const val TEXT_MODEL = "gemini-3.6-flash"
    const val IMAGE_MODEL_PRIMARY = "gemini-nano-banana-2.1"
    const val IMAGE_MODEL_FALLBACK = "gemini-3.1-flash-image"
    const val IMAGE_MODEL_LITE = "gemini-3.1-flash-lite-image"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .callTimeout(200, TimeUnit.SECONDS)
        .addInterceptor(RedactingHttpLogger())
        .build()

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}
