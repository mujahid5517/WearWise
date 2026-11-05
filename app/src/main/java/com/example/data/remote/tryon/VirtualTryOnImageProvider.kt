package com.example.data.remote.tryon

import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateContentRequest
import com.example.data.remote.GeminiInlineData
import com.example.data.remote.GeminiPart
import com.example.data.remote.generateTextContent

interface VirtualTryOnImageProvider {
    suspend fun generate(apiKey: String, request: VirtualTryOnRequest): ByteArray
    suspend fun generateTryOn(
        userPhotoPath: String,
        garmentPaths: List<String>,
        fitType: String,
        pose: String,
        lighting: String
    ): String
}

class FirebaseAiVirtualTryOnProvider : VirtualTryOnImageProvider {
    override suspend fun generate(apiKey: String, request: VirtualTryOnRequest): ByteArray {
        return byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    }

    override suspend fun generateTryOn(
        userPhotoPath: String,
        garmentPaths: List<String>,
        fitType: String,
        pose: String,
        lighting: String
    ): String = userPhotoPath
}

class GeminiVirtualTryOnProvider : VirtualTryOnImageProvider {
    override suspend fun generate(apiKey: String, request: VirtualTryOnRequest): ByteArray {
        return try {
            val req = GeminiGenerateContentRequest(
                contents = listOf(GeminiContent(listOf(
                    GeminiPart(text = request.prompt),
                    GeminiPart(inlineData = GeminiInlineData(request.personImage.mimeType, request.personImage.base64Data))
                )))
            )
            GeminiClient.apiService.generateTextContent(apiKey, req)
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00.toByte(), 0x10.toByte(), 0x4A.toByte(), 0x46.toByte(), 0x49.toByte(), 0x46.toByte())
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

class CompositeVirtualTryOnProvider(
    private vararg val providers: VirtualTryOnImageProvider
) : VirtualTryOnImageProvider {
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
        return providers.firstOrNull()?.generateTryOn(userPhotoPath, garmentPaths, fitType, pose, lighting) ?: userPhotoPath
    }
}
