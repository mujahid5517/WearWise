package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.ApiKeyManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.TryOnPreviewEntity
import com.example.data.mapper.toDomain
import com.example.data.media.EncodedTryOnImage
import com.example.data.media.TryOnFileStore
import com.example.data.remote.tryon.CompositeVirtualTryOnProvider
import com.example.data.remote.tryon.FirebaseAiVirtualTryOnProvider
import com.example.data.remote.tryon.GeminiVirtualTryOnProvider
import com.example.data.remote.tryon.TryOnCloudStore
import com.example.data.remote.tryon.TryOnPromptBuilder
import com.example.data.remote.tryon.VirtualTryOnImageProvider
import com.example.data.remote.tryon.VirtualTryOnRequest
import com.example.data.sync.SyncManager
import com.example.domain.model.SelectedTryOnGarment
import com.example.domain.model.TryOnGenerationException
import com.example.domain.model.TryOnPreview
import com.example.domain.repository.TryOnRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TryOnRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase,
    private val apiKeyManager: ApiKeyManager,
    private val syncManager: SyncManager,
    private val fileStore: TryOnFileStore = TryOnFileStore(context),
    private val imageProvider: VirtualTryOnImageProvider = CompositeVirtualTryOnProvider(
        FirebaseAiVirtualTryOnProvider(),
        GeminiVirtualTryOnProvider()
    ),
    private val cloudStore: TryOnCloudStore? = null
) : TryOnRepository {

    override fun getAllTryOnPreviews(): Flow<List<TryOnPreview>> {
        return database.tryOnPreviewDao().getAllTryOnPreviews().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun deleteTryOnPreview(id: Long) {
        val existing = database.tryOnPreviewDao().getTryOnPreviewById(id)
        existing?.generatedImagePath?.let { fileStore.deleteIfOwned(it) }
        existing?.userPhotoPath?.let { fileStore.deleteIfOwned(it) }
        database.tryOnPreviewDao().deleteTryOnPreviewById(id)
        syncManager.enqueue("DELETE", "tryon", id)
        runCatching { cloudStore?.delete(id) }
    }

    override suspend fun setSavedLook(id: Long, saved: Boolean) {
        database.tryOnPreviewDao().setSavedLook(id, saved)
        syncManager.enqueue("UPDATE", "tryon", id)
        runCatching { cloudStore?.setSavedLook(id, saved) }
    }

    override suspend fun persistPersonPhoto(uri: String): String = withContext(Dispatchers.IO) {
        fileStore.persistPersonPhoto(Uri.parse(uri)).getOrElse { throw wrapImageError(it) }
    }

    override suspend fun persistGarmentPhoto(uri: String): String = withContext(Dispatchers.IO) {
        fileStore.persistGarmentPhoto(Uri.parse(uri)).getOrElse { throw wrapImageError(it) }
    }

    override suspend fun persistPersonBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        fileStore.persistBitmap(bitmap, "person").getOrElse { throw wrapImageError(it) }
    }

    override suspend fun adjustPersonPhoto(path: String, rotationDegrees: Int): String =
        withContext(Dispatchers.IO) {
            fileStore.rotateAndCropToPortrait(path, rotationDegrees).getOrElse { throw wrapImageError(it) }
        }

    override suspend fun rotatePersonPhoto(path: String, rotationDegrees: Int): String =
        withContext(Dispatchers.IO) {
            fileStore.rotatePhoto(path, rotationDegrees).getOrElse { throw wrapImageError(it) }
        }

    override suspend fun cropPersonPhoto(
        path: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): String = withContext(Dispatchers.IO) {
        fileStore.cropNormalized(path, left, top, right, bottom).getOrElse { throw wrapImageError(it) }
    }

    override suspend fun saveGeneratedToGallery(path: String): Uri = withContext(Dispatchers.IO) {
        fileStore.saveToGallery(path).getOrElse { throw wrapImageError(it) }
    }

    override fun shareUriForGenerated(path: String): Uri = fileStore.shareUriForPath(path)

    override suspend fun generateTryOn(
        outfitId: Long,
        fitType: String,
        pose: String,
        lighting: String,
        userPhotoUri: String,
        garments: List<SelectedTryOnGarment>
    ): TryOnPreview = withContext(Dispatchers.IO) {
        val apiKey = apiKeyManager.getEffectiveApiKey()
        if (userPhotoUri.isBlank()) {
            throw TryOnGenerationException.InvalidImage("Add a personal photo before generating a virtual try-on.")
        }
        if (garments.isEmpty()) {
            throw TryOnGenerationException.InvalidImage("Select at least one clothing item.")
        }

        val personImage = try {
            fileStore.encodeForApi(userPhotoUri)
        } catch (e: Exception) {
            throw wrapImageError(e)
        }

        val clothingImages = garments.mapNotNull { garment -> encodeGarment(garment) }
        if (clothingImages.isEmpty()) {
            throw TryOnGenerationException.InvalidImage(
                "Could not read the selected clothing images. Upload a clothing photo or choose a wardrobe item that has an image."
            )
        }
        val garmentsText = garments.joinToString("; ") { "${it.category}: ${it.name}" }
        val prompt = TryOnPromptBuilder.build(
            garmentsText,
            fitType,
            pose,
            lighting,
            clothingImages.size
        )

        val bytes = imageProvider.generate(
            apiKey,
            VirtualTryOnRequest(
                personImage = personImage,
                clothingImages = clothingImages,
                prompt = prompt
            )
        )
        if (bytes.isEmpty()) {
            throw TryOnGenerationException.NoImageReturned()
        }

        val generatedPath = fileStore.persistGeneratedBytes(bytes)
        val previewEntity = TryOnPreviewEntity(
            outfitId = outfitId,
            userPhotoPath = userPhotoUri,
            generatedImagePath = generatedPath,
            previewImageRes = null,
            harmonyScore = when (fitType) {
                "Tailored" -> 96
                "Slim" -> 94
                else -> 91
            },
            fitType = fitType,
            pose = pose,
            lighting = lighting,
            activePiecesCount = garments.size,
            isSavedLook = false,
            garmentSummary = garmentsText,
            createdAt = System.currentTimeMillis()
        )
        val id = database.tryOnPreviewDao().insertTryOnPreview(previewEntity)
        syncManager.enqueue("CREATE", "tryon", id)
        val saved = previewEntity.copy(id = id).toDomain()
        runCatching { cloudStore?.upsertMetadata(saved) }
        saved
    }

    private fun encodeGarment(garment: SelectedTryOnGarment): EncodedTryOnImage? {
        val uri = garment.imageUri
        if (!uri.isNullOrBlank()) {
            return try {
                fileStore.encodeForApi(uri)
            } catch (_: Exception) {
                null
            }
        }
        val remoteUrl = garment.imageUrl
        if (!remoteUrl.isNullOrBlank()) {
            return try {
                val bytes = java.net.URL(remoteUrl).openStream().use { it.readBytes() }
                val file = java.io.File(context.cacheDir, "wardrobe_${System.currentTimeMillis()}.jpg")
                file.writeBytes(bytes)
                fileStore.encodeForApi(file.absolutePath)
            } catch (_: Exception) {
                null
            }
        }
        val res = garment.imageRes ?: garment.catalogAssetName?.let {
            context.resources.getIdentifier(it, "drawable", context.packageName).takeIf { id -> id != 0 }
        } ?: return null
        return fileStore.encodeDrawable(res)
    }

    private fun wrapImageError(error: Throwable): TryOnGenerationException {
        return if (error is TryOnGenerationException) {
            error
        } else {
            TryOnGenerationException.InvalidImage(
                error.message ?: "The selected image could not be processed."
            )
        }
    }
}
