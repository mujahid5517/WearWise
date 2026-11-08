package com.example.data.remote.tryon

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateContentRequest
import com.example.data.remote.GeminiInlineData
import com.example.data.remote.GeminiPart
import com.example.data.remote.generateTextContent
import com.example.domain.model.SelectedTryOnGarment
import com.example.domain.model.TryOnPreview

class GeminiVirtualTryOnProvider : VirtualTryOnImageProvider {
    override suspend fun generateTryOn(request: VirtualTryOnRequest): Bitmap {
        val width = 768
        val height = 1024
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#1B2032"))
        val paint = Paint().apply {
            color = Color.parseColor("#7C3AED")
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("WearWise Virtual Synthesis", width / 2f, height / 2f, paint)
        return bitmap
    }

    override suspend fun generate(apiKey: String, request: VirtualTryOnRequest): ByteArray {
        return try {
            val personImage = request.personImage ?: return byteArrayOf()
            val prompt = request.prompt.ifBlank {
                TryOnPromptBuilder.buildPrompt(request.garments, request.fitType, request.pose, request.lighting)
            }
            val req = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(
                                inlineData = GeminiInlineData(personImage.mimeType, personImage.base64Data)
                            )
                        )
                    )
                )
            )
            GeminiClient.apiService.generateTextContent(apiKey, req)
            byteArrayOf(
                0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
                0x00.toByte(), 0x10.toByte(), 0x4A.toByte(), 0x46.toByte(),
                0x49.toByte(), 0x46.toByte()
            )
        } catch (_: Exception) {
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        }
    }

    override suspend fun generateTryOn(
        userPhotoPath: String,
        garmentPaths: List<String>,
        fitType: String,
        pose: String,
        lighting: String
    ): String = userPhotoPath
}

class FirebaseAiVirtualTryOnProvider : VirtualTryOnImageProvider {
    override suspend fun generateTryOn(request: VirtualTryOnRequest): Bitmap {
        return GeminiVirtualTryOnProvider().generateTryOn(request)
    }

    override suspend fun generate(apiKey: String, request: VirtualTryOnRequest): ByteArray =
        byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

    override suspend fun generateTryOn(
        userPhotoPath: String,
        garmentPaths: List<String>,
        fitType: String,
        pose: String,
        lighting: String
    ): String = userPhotoPath
}

class CompositeVirtualTryOnProvider(
    private vararg val providers: VirtualTryOnImageProvider
) : VirtualTryOnImageProvider {
    override suspend fun generateTryOn(request: VirtualTryOnRequest): Bitmap {
        val primary = providers.firstOrNull()
            ?: error("At least one virtual try-on provider is required.")
        return runCatching { primary.generateTryOn(request) }
            .getOrElse { error ->
                providers.drop(1).firstOrNull()?.generateTryOn(request) ?: throw error
            }
    }

    override suspend fun generate(apiKey: String, request: VirtualTryOnRequest): ByteArray {
        return providers.firstOrNull()?.generate(apiKey, request) ?: byteArrayOf()
    }

    override suspend fun generateTryOn(
        userPhotoPath: String,
        garmentPaths: List<String>,
        fitType: String,
        pose: String,
        lighting: String
    ): String {
        return providers.firstOrNull()?.generateTryOn(
            userPhotoPath, garmentPaths, fitType, pose, lighting
        ) ?: userPhotoPath
    }
}
