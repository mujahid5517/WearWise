package com.example.data.remote

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.IOException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @param:Json(name = "mime_type") val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = null
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

@JsonClass(generateAdapter = true)
data class GeminiModelInfo(
    val name: String? = null,
    val baseModelId: String? = null,
    val supportedGenerationMethods: List<String>? = null,
    val inputTokenLimit: Int? = null,
    val outputTokenLimit: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInteractionRequest(
    val model: String,
    val input: List<GeminiInteractionInput>,
    val store: Boolean = false,
    @param:Json(name = "system_instruction") val systemInstruction: String? = null,
    @param:Json(name = "generation_config") val generationConfig: GeminiInteractionGenerationConfig? = null,
    @param:Json(name = "response_format") val responseFormat: GeminiImageResponseFormat? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInteractionInput(
    val type: String,
    val text: String? = null,
    val data: String? = null,
    @param:Json(name = "mime_type") val mimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInteractionGenerationConfig(
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageResponseFormat(
    val type: String = "image",
    @param:Json(name = "mime_type") val mimeType: String = "image/jpeg",
    @param:Json(name = "aspect_ratio") val aspectRatio: String = "3:4",
    @param:Json(name = "image_size") val imageSize: String = "1K"
)

@JsonClass(generateAdapter = true)
data class GeminiInteractionResponse(
    val status: String? = null,
    val steps: List<GeminiInteractionStep>? = null,
    @param:Json(name = "output_text") val outputText: String? = null,
    @param:Json(name = "output_image") val outputImage: GeminiInteractionContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInteractionStep(
    val type: String? = null,
    val content: List<GeminiInteractionContent>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInteractionContent(
    val type: String? = null,
    val text: String? = null,
    val data: String? = null,
    @param:Json(name = "mime_type") val mimeType: String? = null
)

interface GeminiApiService {
    @GET("v1beta/models/{model}")
    suspend fun getModel(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String
    ): GeminiModelInfo

    @POST("v1beta/interactions")
    @Headers("Api-Revision: 2026-05-20")
    suspend fun createInteraction(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiInteractionRequest
    ): GeminiInteractionResponse
}

suspend fun GeminiApiService.generateTextContent(
    apiKey: String,
    request: GeminiGenerateContentRequest
): GeminiGenerateContentResponse = GeminiClient.generateTextContent(apiKey, request)

private class RedactingHttpLogger : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val started = System.currentTimeMillis()
        return try {
            val response = chain.proceed(request)
            Log.d(
                "WearWiseGemini",
                "HTTP ${response.code} ${request.url.encodedPath} (${System.currentTimeMillis() - started}ms)"
            )
            response
        } catch (e: IOException) {
            Log.w("WearWiseGemini", "HTTP failed ${request.url.encodedPath}")
            throw e
        }
    }
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    const val TEXT_MODEL = "gemini-3.8-flash"
    const val IMAGE_MODEL = "gemini-nano-banana-2.1"

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

    suspend fun verifyConnection(apiKey: String): String {
        val key = apiKey.trim()
        require(key.isNotEmpty()) { "API key cannot be empty." }

        val model = apiService.getModel(IMAGE_MODEL, key)
        val returnedModelId = model.baseModelId ?: model.name?.substringAfterLast('/')
        if (!returnedModelId.equals(IMAGE_MODEL, ignoreCase = true)) {
            throw IllegalStateException("Gemini did not confirm availability of $IMAGE_MODEL.")
        }

        val response = apiService.createInteraction(
            key,
            GeminiInteractionRequest(
                model = TEXT_MODEL,
                input = listOf(GeminiInteractionInput(type = "text", text = "Reply with OK."))
            )
        )
        val reply = response.outputText
            ?: response.steps.orEmpty()
                .flatMap { it.content.orEmpty() }
                .firstOrNull { it.type == "text" }
                ?.text
        if (reply.isNullOrBlank()) {
            throw IllegalStateException("Gemini connection responded without text. No image was generated.")
        }
        return "API key valid; $IMAGE_MODEL is available. Gemini text endpoint connected. No image generated."
    }

    suspend fun analyzeClothing(
        apiKey: String,
        prompt: String,
        imageMimeType: String,
        imageBase64: String
    ): String {
        val response = generateTextContent(
            apiKey,
            GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(inlineData = GeminiInlineData(imageMimeType, imageBase64))
                        )
                    )
                )
            )
        )
        return response.candidates?.firstOrNull()?.content?.parts
            ?.firstOrNull { !it.text.isNullOrBlank() }?.text
            ?: throw IllegalStateException("Gemini returned no clothing analysis text.")
    }

    suspend fun generateTextContent(
        apiKey: String,
        request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse {
        val input = request.contents.flatMap { content ->
            content.parts.mapNotNull { part ->
                when {
                    part.text != null -> GeminiInteractionInput(type = "text", text = part.text)
                    part.inlineData != null -> GeminiInteractionInput(
                        type = "image",
                        data = part.inlineData.data,
                        mimeType = part.inlineData.mimeType
                    )
                    else -> null
                }
            }
        }
        require(input.isNotEmpty()) { "Gemini request must include text or image input." }

        val response = apiService.createInteraction(
            apiKey.trim(),
            GeminiInteractionRequest(
                model = TEXT_MODEL,
                input = input,
                systemInstruction = request.systemInstruction?.parts
                    ?.mapNotNull { it.text }
                    ?.joinToString("\n")
                    ?.takeIf(String::isNotBlank),
                generationConfig = request.generationConfig?.temperature?.let {
                    GeminiInteractionGenerationConfig(temperature = it)
                }
            )
        )
        val parts = response.steps.orEmpty()
            .flatMap { it.content.orEmpty() }
            .mapNotNull { content ->
                when {
                    content.text != null -> GeminiPart(text = content.text)
                    content.data != null && content.mimeType != null ->
                        GeminiPart(inlineData = GeminiInlineData(content.mimeType, content.data))
                    else -> null
                }
            }
        val allParts = if (parts.isEmpty() && !response.outputText.isNullOrBlank()) {
            listOf(GeminiPart(text = response.outputText))
        } else {
            parts
        }
        return GeminiGenerateContentResponse(
            candidates = listOf(GeminiCandidate(content = GeminiContent(allParts)))
        )
    }

    suspend fun generateTryOnImage(
        apiKey: String,
        prompt: String,
        personMimeType: String,
        personBase64: String,
        garmentImages: List<GeminiInlineData>
    ): Pair<String, ByteArray> {
        require(apiKey.isNotBlank()) { "Add a Gemini API key in Profile before generating a try-on." }
        require(garmentImages.isNotEmpty()) { "Select at least one garment image." }
        require(garmentImages.size <= 13) {
            "Gemini supports up to 14 reference images for this model; this includes your person photo."
        }
        require(personBase64.isNotBlank() && personMimeType.startsWith("image/")) {
            "The person reference must be a valid image."
        }
        require(garmentImages.all { it.data.isNotBlank() && it.mimeType.startsWith("image/") }) {
            "Every selected garment must have a valid image."
        }

        val response = apiService.createInteraction(
            apiKey.trim(),
            buildTryOnRequest(prompt, personMimeType, personBase64, garmentImages)
        )
        return parseGeneratedImage(response)
    }

    internal fun buildTryOnRequest(
        prompt: String,
        personMimeType: String,
        personBase64: String,
        garmentImages: List<GeminiInlineData>
    ): GeminiInteractionRequest {
        require(garmentImages.isNotEmpty()) { "Select at least one garment image." }
        require(garmentImages.size <= 13) {
            "Gemini supports up to 14 reference images for this model; this includes your person photo."
        }
        val inputs = buildList {
            add(GeminiInteractionInput(type = "text", text = prompt))
            add(GeminiInteractionInput(type = "image", data = personBase64, mimeType = personMimeType))
            garmentImages.forEach { image ->
                add(GeminiInteractionInput(type = "image", data = image.data, mimeType = image.mimeType))
            }
        }
        return GeminiInteractionRequest(
            model = IMAGE_MODEL,
            input = inputs,
            responseFormat = GeminiImageResponseFormat()
        )
    }

    internal fun parseGeneratedImage(response: GeminiInteractionResponse): Pair<String, ByteArray> {
        val image = response.outputImage
            ?: response.steps.orEmpty()
                .flatMap { it.content.orEmpty() }
                .lastOrNull { it.type == "image" || it.mimeType?.startsWith("image/") == true }
            ?: throw IllegalStateException("Gemini completed without returning an image.")
        val mimeType = image.mimeType?.lowercase()
            ?: throw IllegalStateException("Gemini image response did not include a MIME type.")
        require(mimeType == "image/jpeg" || mimeType == "image/png") {
            "Gemini returned an unsupported image MIME type: $mimeType."
        }
        val imageData = image.data?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Gemini image response did not contain image data.")
        val bytes = try {
            java.util.Base64.getMimeDecoder().decode(imageData)
        } catch (error: IllegalArgumentException) {
            throw IllegalStateException("Gemini returned invalid base64 image data.", error)
        }
        require(bytes.isNotEmpty()) { "Gemini returned an empty image." }
        return mimeType to bytes
    }

    fun describeHttpError(error: HttpException, operation: String): String {
        val body = error.response()?.errorBody()?.string()
        val detail = body?.let {
            runCatching { moshi.adapter(GeminiErrorEnvelope::class.java).fromJson(it)?.error }
                .getOrNull()
        }
        val message = detail?.message.orEmpty().take(240)
        val suffix = message.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
        return when (error.code()) {
            400 -> if (
                detail?.status.equals("API_KEY_INVALID", ignoreCase = true) ||
                message.contains("api key not valid", ignoreCase = true) ||
                message.contains("invalid api key", ignoreCase = true)
            ) {
                "Invalid Gemini API key (HTTP 400). Check the key and try again."
            } else if (
                message.contains("model", ignoreCase = true) &&
                listOf("not found", "not supported", "unsupported", "invalid").any {
                    message.contains(it, ignoreCase = true)
                }
            ) {
                "Unsupported Gemini model for $operation (HTTP 400). Check the selected model ID.$suffix"
            } else {
                "Gemini rejected the $operation request (HTTP 400). Check the request format.$suffix"
            }
            401 -> "Invalid Gemini API key (HTTP 401). Check the key and try again."
            403 -> if (
                message.contains("api key not valid", ignoreCase = true) ||
                message.contains("invalid api key", ignoreCase = true)
            ) {
                "Invalid Gemini API key (HTTP 403). Check the key and try again."
            } else {
                "Gemini access denied (HTTP 403). Check project permissions and billing.$suffix"
            }
            404 -> "Gemini model or endpoint unavailable for $operation (HTTP 404). Verify the model ID and API endpoint.$suffix"
            429 -> if (detail?.status.equals("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                message.contains("quota", ignoreCase = true)
            ) {
                "Gemini quota exceeded (HTTP 429). Wait for the quota reset; this request was not retried.$suffix"
            } else {
                "Gemini rate limit reached (HTTP 429). Wait before trying again.$suffix"
            }
            in 500..599 -> "Gemini service temporarily unavailable (HTTP ${error.code()})."
            else -> "Gemini $operation failed (HTTP ${error.code()}).$suffix"
        }
    }

    fun describeConnectionFailure(error: Throwable, apiKey: String, operation: String): String {
        val message = when (error) {
            is HttpException -> describeHttpError(error, operation)
            is IOException -> "Unable to connect to Gemini. Check your internet connection and try again."
            else -> error.localizedMessage ?: "Gemini $operation failed."
        }
        return message.replace(apiKey, "[redacted]")
    }
}
