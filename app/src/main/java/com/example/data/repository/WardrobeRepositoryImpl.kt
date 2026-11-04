package com.example.data.repository

import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.media.TryOnFileStore
import com.example.data.remote. CloudWardrobeRecord
import com.example.data.remote.WardrobeCloudStore
import com.example.data.sync.SyncManager
import com.example.domain.model.ClothingItem
import com.example.domain.repository.WardrobeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class WardrobeRepositoryImpl(
    private val database: AppDatabase,
    private val syncManager: SyncManager,
    private val fileStore: TryOnFileStore? = null,
    private val cloudStore: WardrobeCloudStore? = null,
    connectivity: Flow<Boolean>? = null
) : WardrobeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _syncMessage = MutableStateFlow<String?>(null)
    override val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    override val uploadProgress: StateFlow<Float?> = cloudStore?.uploadProgress ?: MutableStateFlow(null)

    init {
        scope.launch { syncFromCloud() }
        cloudStore?.observeAuthState { scope.launch { syncFromCloud(); syncPendingItems() } }
        connectivity?.distinctUntilChanged()?.filter { it }?.onEach { syncPendingItems() }?.launchIn(scope)
    }

    override fun getAllClothingItems(): Flow<List<ClothingItem>> {
        val localItems = database.clothingItemDao().getAllClothingItems().map { list -> list.map { it.toDomain() } }
        val uid = cloudStore?.signedInUid ?: MutableStateFlow("")
        return combine(localItems, uid) { items, owner -> items.filter { it.ownerUid == owner } }
    }

    override fun getClothingItemsByCategory(category: String): Flow<List<ClothingItem>> =
        if (category == "All Items" || category.isEmpty()) getAllClothingItems()
        else database.clothingItemDao().getClothingItemsByCategory(category).map { list -> list.map { it.toDomain() } }

    override suspend fun addClothingItem(item: ClothingItem): Long {
        val now = System.currentTimeMillis()
        val prepared = item.copy(
            ownerUid = cloudStore?.signedInUid?.value.orEmpty(),
            remoteId = item.remoteId ?: UUID.randomUUID().toString(),
            createdAt = now,
            updatedAt = now
        )
        val id = database.clothingItemDao().insertClothingItem(prepared.toEntity())
        syncManager.enqueue("CREATE", "wardrobe", id)
        syncOne(prepared.copy(id = id))
        return id
    }

    override suspend fun updateClothingItem(item: ClothingItem) {
        val previous = database.clothingItemDao().getClothingItemById(item.id)?.toDomain()
        val updated = item.copy(updatedAt = System.currentTimeMillis())
        database.clothingItemDao().updateClothingItem(updated.toEntity())
        if (previous?.imageUri != updated.imageUri) previous?.imageUri?.let { fileStore?.deleteIfOwned(it) }
        syncManager.enqueue("UPDATE", "wardrobe", updated.id)
        syncOne(updated)
    }

    override suspend fun deleteClothingItem(id: Long) {
        val item = database.clothingItemDao().getClothingItemById(id)?.toDomain()
        val store = cloudStore
        if (item != null && !item.remoteId.isNullOrBlank() && item.ownerUid.isNotBlank()) {
            if (store?.signedInUid?.value != item.ownerUid) {
                _syncMessage.value = "Sign into the account that owns this item before deleting its cloud copy."
                return
            }
            try {
                store.delete(item)
            } catch (error: Exception) {
                _syncMessage.value = "Could not delete the cloud item: ${error.message.orEmpty()}"
                return
            }
        }
        database.clothingItemDao().deleteClothingItemById(id)
        item?.imageUri?.let { fileStore?.deleteIfOwned(it) }
        syncManager.enqueue("DELETE", "wardrobe", id)
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        database.clothingItemDao().toggleFavorite(id, isFavorite)
        syncManager.enqueue("UPDATE", "wardrobe", id)
        database.clothingItemDao().getClothingItemById(id)?.toDomain()?.let { syncOne(it.copy(isFavorite = isFavorite)) }
    }

    override suspend fun persistClothingImage(uri: String): String = withContext(Dispatchers.IO) {
        val store = fileStore ?: error("Image storage is unavailable.")
        store.persistGarmentPhoto(Uri.parse(uri)).getOrElse { throw it }
    }

    private suspend fun syncOne(item: ClothingItem) {
        val store = cloudStore ?: return
        val activeUid = store.signedInUid.value
        if (activeUid.isBlank()) {
            _syncMessage.value = "Saved on this device. Sign in to sync it to your account."
            return
        }
        if (item.ownerUid != activeUid) return
        try {
            val cloudImage = store.upsert(item)
            database.clothingItemDao().updateClothingItem(
                item.copy(imageUrl = "", imageStoragePath = cloudImage.storagePath).toEntity()
            )
            _syncMessage.value = "Wardrobe item synced to your account."
        } catch (error: Exception) {
            _syncMessage.value = "Saved locally; cloud sync failed: ${error.message ?: "sign in to sync"}"
        }
    }

    private suspend fun syncFromCloud() {
        val store = cloudStore ?: return
        try {
            store.getAll().forEach { remote ->
                val dao = database.clothingItemDao()
                val local = dao.getClothingItemByRemoteId(remote.remoteId.orEmpty())?.toDomain()
                val localImage = local?.imageUri ?: downloadRemoteImage(remote)
                val item = remote.toDomain(local).copy(imageUri = localImage)
                if (local == null) dao.insertClothingItem(item.toEntity())
                else dao.updateClothingItem(item.copy(id = local.id, imageUri = local.imageUri).toEntity())
            }
        } catch (error: Exception) {
            _syncMessage.value = "Could not refresh cloud wardrobe: ${error.message.orEmpty()}"
        }
    }

    private suspend fun downloadRemoteImage(remote: CloudWardrobeRecord): String? {
        val path = remote.imageStoragePath?.takeIf { it.isNotBlank() } ?: return null
        val store = fileStore ?: return null
        val destination = java.io.File(store.storageDirectory(), "wardrobe_${remote.remoteId.orEmpty()}.jpg")
        return runCatching {
            cloudStore?.downloadImage(path, destination) ?: return null
            destination.absolutePath
        }.getOrNull()
    }

    private suspend fun syncPendingItems() {
        val uid = cloudStore?.signedInUid?.value.orEmpty()
        if (uid.isBlank()) return
        database.clothingItemDao().getAllClothingItemsSnapshot()
            .map { it.toDomain() }
            .filter { it.ownerUid == uid && !it.remoteId.isNullOrBlank() }
            .forEach { syncOne(it) }
    }

    private fun CloudWardrobeRecord.toDomain(local: ClothingItem?): ClothingItem = ClothingItem(
        id = local?.id ?: 0L,
        userId = local?.userId ?: 1L,
        ownerUid = ownerUid,
        name = name,
        category = category,
        subcategory = subcategory,
        color = color,
        pattern = pattern,
        brand = brand,
        size = size,
        season = season,
        occasion = occasion,
        description = description,
        style = local?.style ?: occasion,
        formality = local?.formality ?: "Everyday",
        formalityScore = local?.formalityScore ?: 5,
        fabric = local?.fabric.orEmpty(),
        isFavorite = isFavorite,
        imageRes = null,
        imageUri = local?.imageUri,
        imageUrl = imageUrl,
        imageStoragePath = imageStoragePath,
        catalogAssetName = catalogAssetName,
        remoteId = remoteId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        wearCount = local?.wearCount ?: 0,
        lastWornDate = local?.lastWornDate
    )
}
