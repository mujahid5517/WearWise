package com.example.data.remote

import com.example.domain.model.ClothingItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

data class CloudWardrobeRecord(
    val remoteId: String = "",
    val ownerUid: String = "",
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
    val imageUrl: String = "",
    val imageStoragePath: String = "",
    val catalogAssetName: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class CloudImageResult(val storagePath: String, val downloadUrl: String)

class WardrobeCloudStore(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    val signedInUid = MutableStateFlow(auth.currentUser?.uid.orEmpty())
    val uploadProgress = MutableStateFlow<Float?>(null)

    fun observeAuthState(listener: () -> Unit) {
        auth.addAuthStateListener {
            signedInUid.value = it.currentUser?.uid.orEmpty()
            listener()
        }
    }

    suspend fun getAll(): List<CloudWardrobeRecord> = emptyList()
    suspend fun upsert(item: ClothingItem): CloudImageResult = CloudImageResult("", "")
    suspend fun delete(item: ClothingItem) {}
    suspend fun downloadImage(storagePath: String, destination: File) {}
}
