package com.example.data.remote

import android.net.Uri
import com.example.domain.model.ClothingItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

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

    suspend fun getAll(): List<CloudWardrobeRecord> {
        val userId = auth.currentUser?.uid.orEmpty()
        if (userId.isBlank()) return emptyList()
        val collection = firestore.collection("wardrobe_items")
        val document = collection.document(userId)
        val docSnapshot = document.get().await()
        val items = mutableListOf<CloudWardrobeRecord>()
        (docSnapshot.data?.get("items") as? List<Map<String, Any>>)?.forEach { itemMap ->
            items.add(
                CloudWardrobeRecord(
                    remoteId = itemMap["remoteId"]?.toString() ?: "",
                    ownerUid = userId,
                    name = itemMap["name"]?.toString() ?: "",
                    category = itemMap["category"]?.toString() ?: "",
                    subcategory = itemMap["subcategory"]?.toString() ?: "",
                    color = itemMap["color"]?.toString() ?: "",
                    pattern = itemMap["pattern"]?.toString() ?: "",
                    brand = itemMap["brand"]?.toString() ?: "",
                    size = itemMap["size"]?.toString() ?: "",
                    season = itemMap["season"]?.toString() ?: "",
                    occasion = itemMap["occasion"]?.toString() ?: "",
                    description = itemMap["description"]?.toString() ?: "",
                    isFavorite = itemMap["isFavorite"]?.toString() == "true",
                    imageUrl = itemMap["imageUrl"]?.toString() ?: "",
                    imageStoragePath = itemMap["imageStoragePath"]?.toString() ?: "",
                    catalogAssetName = itemMap["catalogAssetName"]?.toString(),
                    createdAt = (itemMap["createdAt"] as? Number)?.toLong() ?: 0L,
                    updatedAt = (itemMap["updatedAt"] as? Number)?.toLong() ?: 0L
                )
            )
        }
        return items
    }

    suspend fun upsert(item: ClothingItem): CloudImageResult {
        val userId = auth.currentUser?.uid.orEmpty()
        if (userId.isBlank()) return CloudImageResult("", "")
        val remoteId = item.remoteId.orEmpty().ifBlank { UUID.randomUUID().toString() }
        val category = item.category
        val name = item.name
        val color = item.color
        val pattern = item.pattern
        val brand = item.brand
        val size = item.size
        val season = item.season
        val occasion = item.occasion
        val description = item.description
        val isFavorite = item.isFavorite
        val imageUri = item.imageUri.orEmpty()
        val catalogAssetName = item.catalogAssetName

        var storagePath = ""
        var downloadUrl = ""

        if (imageUri.isNotBlank()) {
            try {
                val storageRef = storage.reference.child("wardrobe/$userId/${remoteId}.jpg")
                if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) {
                    storageRef.putFile(Uri.parse(imageUri)).await()
                } else {
                    val file = File(imageUri)
                    if (file.exists()) {
                        storageRef.putFile(Uri.fromFile(file)).await()
                    }
                }
                downloadUrl = storageRef.downloadUrl.await().toString()
                storagePath = storageRef.path
            } catch (_: Exception) {
                // Upload failed - continue without cloud storage
            }
        }

        val itemMap = mapOf(
            "remoteId" to remoteId,
            "name" to name,
            "category" to category,
            "subcategory" to item.subcategory,
            "color" to color,
            "pattern" to pattern,
            "brand" to brand,
            "size" to size,
            "season" to season,
            "occasion" to occasion,
            "description" to description,
            "isFavorite" to isFavorite,
            "imageUrl" to downloadUrl,
            "imageStoragePath" to storagePath,
            "catalogAssetName" to catalogAssetName,
            "createdAt" to item.createdAt,
            "updatedAt" to item.updatedAt
        )

        val collection = firestore.collection("wardrobe_items")
        val document = collection.document(userId)
        document.set(itemMap).await()

        return CloudImageResult(storagePath = storagePath, downloadUrl = downloadUrl)
    }

    suspend fun delete(item: ClothingItem) {
        val userId = auth.currentUser?.uid.orEmpty()
        val remoteId = item.remoteId.orEmpty()
        if (remoteId.isNotBlank() && userId.isNotBlank()) {
            runCatching {
                val storageRef = storage.reference.child("wardrobe/$userId/wardrobe_$remoteId.jpg")
                storageRef.delete().await()
            }
            val collection = firestore.collection("wardrobe_items")
            val document = collection.document(userId)
            document.update("items", FieldValue.arrayRemove(mapOf("remoteId" to remoteId))).await()
        }
    }

    suspend fun downloadImage(storagePath: String, destination: File) {
        val ref = storage.reference.child(storagePath)
        ref.getFile(destination).await()
    }
}
