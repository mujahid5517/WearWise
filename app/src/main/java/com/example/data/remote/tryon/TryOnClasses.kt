package com.example.data.remote.tryon

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.data.media.EncodedTryOnImage
import com.example.domain.model.SelectedTryOnGarment
import com.example.domain.model.TryOnPreview
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class VirtualTryOnRequest(
    val personImage: EncodedTryOnImage? = null,
    val garmentImages: List<EncodedTryOnImage> = emptyList(),
    val garments: List<SelectedTryOnGarment> = emptyList(),
    val fitType: String = "Tailored",
    val pose: String = "Confident Stance",
    val lighting: String = "Warm Daylight",
    val apiKey: String = ""
)

interface VirtualTryOnImageProvider {
    suspend fun generateTryOn(request: VirtualTryOnRequest): Bitmap
}

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
}

class FirebaseAiVirtualTryOnProvider : VirtualTryOnImageProvider {
    override suspend fun generateTryOn(request: VirtualTryOnRequest): Bitmap {
        return GeminiVirtualTryOnProvider().generateTryOn(request)
    }
}

class CompositeVirtualTryOnProvider(
    private val primary: VirtualTryOnImageProvider,
    private val secondary: VirtualTryOnImageProvider
) : VirtualTryOnImageProvider {
    override suspend fun generateTryOn(request: VirtualTryOnRequest): Bitmap {
        return runCatching { primary.generateTryOn(request) }
            .getOrElse { secondary.generateTryOn(request) }
    }
}

class TryOnCloudStore(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val signedInUid: String get() = auth.currentUser?.uid.orEmpty()
    suspend fun delete(id: Long) {}
    suspend fun setSavedLook(id: Long, saved: Boolean) {}
    suspend fun upsertMetadata(preview: TryOnPreview) {}
}

object TryOnPromptBuilder {
    fun buildPrompt(garments: List<SelectedTryOnGarment>, fitType: String, pose: String, lighting: String): String {
        val list = garments.joinToString(", ") { "${it.name} (${it.category})" }
        return "High fashion virtual try-on featuring $list with $fitType fit, $pose posture, under $lighting lighting."
    }
}

object TryOnImageValidator {
    fun validate(bitmap: Bitmap): Boolean = bitmap.width > 0 && bitmap.height > 0
}
