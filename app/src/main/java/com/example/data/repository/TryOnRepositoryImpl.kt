package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.ApiKeyManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.TryOnPreviewEntity
import com.example.data.mapper.toDomain
import com.example.data.media.EncodedTryOnImage
import com.example.data.media.TryOnFileStore
import com.example.data.remote.tryon.GeminiVirtualTryOnProvider
import com.example.data.remote.tryon.TryOnCloudStore
import com.example.data.remote.tryon.TryOnPromptBuilder
import com.example.data.remote.tryon.VirtualTryOnImageProvider
import com.example.data.remote.tryon.VirtualTryOnRequest
import com.example.data.remote.tryon.TryOnImageValidator
import com.example.data.sync.SyncManager
import com.example.domain.model.SelectedTryOnGarment
import com.example.domain.model.TryOnGenerationException
import com.example.domain.model.TryOnPreview
import com.example.domain.model.Outfit
import com.example.domain.model.previewCacheKey
import com.example.domain.repository.TryOnRepository
import com.example.presentation.common.WearWiseDrawables
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
    private val imageProvider: VirtualTryOnImageProvider = GeminiVirtualTryOnProvider(),
    private val cloudStore: TryOnCloudStore? = null
) : TryOnRepository {

    override fun getAllTryOnPreviews(): Flow<List<TryOnPreview>> {
        return database.tryOnPreviewDao().getAllTryOnPreviews().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCachedOutfitPreview(outfitKey: Long): String? = withContext(Dispatchers.IO) {
        database.tryOnPreviewDao().getLatestCatalogImage(outfitKey)?.takeIf { java.io.File(it).isFile }
    }

    /** Generates and Room-caches a full-body catalog try-on using the selected wardrobe images. */
    override suspend fun generateOutfitPreview(outfit: Outfit, modelImageRes: Int): String = withContext(Dispatchers.IO) {
        if (outfit.items.isEmpty()) throw TryOnGenerationException.InvalidImage(
            "Add clothing with photos to your wardrobe before creating a full-body look."
        )
        val modelImage = fileStore.encodeDrawable(context, modelImageRes)
            ?: throw TryOnGenerationException.InvalidImage("The full-body model reference could not be loaded.")
        val garments = outfit.items.take(6).map { item ->
            SelectedTryOnGarment(
                slot = item.category,
                name = item.name,
                category = item.category,
                imageRes = item.imageRes,
                imageUri = item.imageUri,
                clothingItemId = item.id,
                imageUrl = item.imageUrl,
                catalogAssetName = item.catalogAssetName
            )
        }
        val encodedGarments = garments.mapNotNull { encodeGarment(it) }
        val summary = garments.joinToString("; ") { "${it.category}: ${it.name}" }
        val bitmap = imageProvider.generateTryOn(
            VirtualTryOnRequest(
                personImage = modelImage,
                garmentImages = encodedGarments,
                garments = garments,
                fitType = "Catalog",
                pose = "Full-body standing",
                lighting = "Soft studio",
                apiKey = apiKeyManager.getEffectiveApiKey()
            )
        )
        val baos = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val bytes = baos.toByteArray()
        val generatedPath = fileStore.persistGeneratedBytes(bytes).getOrNull() ?: error("Failed to save image")
        val entity = TryOnPreviewEntity(
            outfitId = outfit.id,
            userPhotoPath = null,
            generatedImagePath = generatedPath,
            previewImageRes = null,
            harmonyScore = outfit.score,
            fitType = "Catalog",
            pose = "Full-body standing",
            lighting = "Soft studio",
            activePiecesCount = garments.size,
            isSavedLook = false,
            garmentSummary = summary,
            createdAt = System.currentTimeMillis()
        )
        val id = database.tryOnPreviewDao().insertTryOnPreview(entity)
        syncManager.enqueue("CREATE", "tryon", id)
        runCatching { cloudStore?.upsertMetadata(entity.copy(id = id).toDomain()) }
        generatedPath
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
        if (apiKey.isBlank()) {
            throw TryOnGenerationException.InvalidImage("Add a Gemini API key in Profile before generating a try-on.")
        }

        val personImage = try {
            fileStore.encodeForApi(userPhotoUri)
        } catch (e: Exception) {
            throw wrapImageError(e)
        }

        val clothingImages = garments.map { garment ->
            encodeGarment(garment)
        }
        val garmentsText = garments.joinToString("; ") { "${it.category}: ${it.name}" }
        val prompt = TryOnPromptBuilder.buildPrompt(
            garments,
            fitType,
            pose,
            lighting
        )

        val bitmap = imageProvider.generateTryOn(
            VirtualTryOnRequest(
                personImage = personImage,
                garmentImages = clothingImages,
                garments = garments,
                fitType = fitType,
                pose = pose,
                lighting = lighting,
                apiKey = apiKey
            )
        )
        if (!TryOnImageValidator.validate(bitmap)) {
            throw TryOnGenerationException.NoImageReturned("Gemini returned an invalid image.")
        }
        val baos = java.io.ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)) {
            "Could not encode Gemini's generated image."
        }
        val bytes = baos.toByteArray()
        check(bytes.isNotEmpty()) { "Gemini's generated image was empty." }

        val generatedPath = fileStore.persistGeneratedBytes(bytes)
            .getOrElse { throw IllegalStateException("Could not save Gemini's generated image.", it) }
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

    private fun encodeGarment(garment: SelectedTryOnGarment): EncodedTryOnImage {
        val uri = garment.imageUri
        if (!uri.isNullOrBlank()) {
            return fileStore.encodeForApi(uri)
                ?: error("Could not read the selected ${garment.category} image.")
        }
        val remoteUrl = garment.imageUrl
        if (!remoteUrl.isNullOrBlank()) {
            val connection = java.net.URL(remoteUrl).openConnection().apply {
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            val bytes = connection.getInputStream().use { it.readBytes() }
            val file = java.io.File(context.cacheDir, "wardrobe_${System.currentTimeMillis()}.jpg")
            file.writeBytes(bytes)
            return fileStore.encodeForApi(file.absolutePath)
                ?: error("Could not read the selected ${garment.category} image.")
        }
        val res = garment.imageRes ?: garment.catalogAssetName?.let {
            context.resources.getIdentifier(it, "drawable", context.packageName).takeIf { id -> id != 0 }
        }
        if (res != null) {
            return fileStore.encodeDrawable(context, res)
                ?: error("Could not load the selected ${garment.category} image.")
        }
        error("The selected ${garment.category} item has no usable image.")
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
