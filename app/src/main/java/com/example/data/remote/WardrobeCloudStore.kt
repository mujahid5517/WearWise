package com.example.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.domain.model.ClothingItem
import java.io.File

data class CloudWardrobeRecord(
    val remoteId: String? = null,
    val ownerUid: String = "",
    val storagePath: String = "",
    val name: String = "",
    val category: String = "",
    val subcategory: String = "",
    val color: String = "",
    val pattern: String = "",
    val brand: String = "",
    val size: String = "",
    val season: String = "",
    val occasion: String = "",
    val description: String = "",
    val isFavorite: Boolean = false,
    val imageUrl: String? = null,
    val imageStoragePath: String? = null,
    val catalogAssetName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(local: ClothingItem?): ClothingItem = local ?: ClothingItem(name = name, category = category, color = color, style = occasion)
}

class WardrobeCloudStore(
    private val auth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?,
    private val storage: FirebaseStorage?
) {
    val uploadProgress = MutableStateFlow<Float?>(null)
    val signedInUid: StateFlow<String> = MutableStateFlow(auth?.currentUser?.uid.orEmpty())

    fun observeAuthState(onAuthChanged: () -> Unit) {
        auth?.addAuthStateListener { onAuthChanged() }
    }

    suspend fun sync() {}

    suspend fun getAll(): List<CloudWardrobeRecord> = emptyList()

    suspend fun upsert(item: ClothingItem): CloudWardrobeRecord = CloudWardrobeRecord(storagePath = item.imageStoragePath.orEmpty())

    suspend fun delete(item: ClothingItem) {}

    suspend fun downloadImage(storagePath: String, targetFile: File) {}
}
