package com.example.data.remote.tryon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiInlineData
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

class GeminiVirtualTryOnProvider : VirtualTryOnImageProvider {
    override suspend fun generateTryOn(request: VirtualTryOnRequest): Bitmap {
        val personImage = request.personImage
            ?: throw IllegalArgumentException("Select a person photo before generating a try-on.")
        if (request.garmentImages.isEmpty()) {
            throw IllegalArgumentException("Select at least one wardrobe garment image.")
        }

        val prompt = request.prompt.ifBlank {
            TryOnPromptBuilder.buildPrompt(request.garments, request.fitType, request.pose, request.lighting)
        }
        val (mimeType, bytes) = try {
            GeminiClient.generateTryOnImage(
                apiKey = request.apiKey,
                prompt = prompt,
                personMimeType = personImage.mimeType,
                personBase64 = personImage.base64Data,
                garmentImages = request.garmentImages.map {
                    GeminiInlineData(it.mimeType, it.base64Data)
                }
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: HttpException) {
            throw IllegalStateException(
                GeminiClient.describeConnectionFailure(error, request.apiKey, "virtual try-on"),
                error
            )
        } catch (error: IOException) {
            throw IllegalStateException(
                "Unable to connect to Gemini. Check your internet connection and try again.",
                error
            )
        }

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Gemini returned $mimeType data that Android could not decode.")
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            throw IllegalStateException("Gemini returned an image with invalid dimensions.")
        }
        return bitmap
    }
}
