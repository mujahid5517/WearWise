package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val firstName: String,
    val nickname: String,
    val email: String,
    val ageRange: String,
    val gender: String,
    val country: String,
    val region: String,
    val profession: String,
    val dressEnvironment: String,
    val stylePreferences: String, // comma-separated or JSON
    val favoriteColors: String,
    val avoidedColors: String,
    val commonOccasions: String,
    val climate: String,
    val silhouetteArchetype: String,
    val profileImage: String?,
    val profileImageRes: Int?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "clothing_items")
data class ClothingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long = 1L,
    val name: String,
    val category: String,
    val color: String,
    val style: String,
    val season: String,
    val formality: String,
    val formalityScore: Int,
    val fabric: String,
    val isFavorite: Boolean,
    val imageRes: Int?,
    val imageUri: String?,
    val wearCount: Int,
    val lastWornDate: String?,
    val createdAt: Long,
    val subcategory: String = "",
    val brand: String = "",
    val size: String = "",
    val occasion: String = "",
    val description: String = "",
    val remoteId: String? = null,
    val imageStoragePath: String? = null,
    val updatedAt: Long = 0L,
    val imageUrl: String? = null,
    val pattern: String = "",
    val ownerUid: String = "",
    val catalogAssetName: String? = null
)

@Entity(tableName = "outfits")
data class OutfitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long = 1L,
    val title: String,
    val occasion: String,
    val style: String,
    val score: Int,
    val rationale: String,
    val weatherTag: String,
    val isFavorite: Boolean,
    val previewImageRes: Int?,
    val previewImageUri: String?,
    val occasionFitScore: Int,
    val thermalSyncScore: Int,
    val colorHarmonyScore: Int,
    val createdAt: Long,
    val ownerUid: String = "",
    @androidx.room.ColumnInfo(defaultValue = "''") val colorCompatibilityExplanation: String = "",
    @androidx.room.ColumnInfo(defaultValue = "''") val occasionSuitability: String = ""
)

@Entity(tableName = "outfit_items", primaryKeys = ["outfitId", "clothingId"])
data class OutfitItemEntity(
    val outfitId: Long,
    val clothingId: Long
)

@Entity(tableName = "try_on_previews")
data class TryOnPreviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long = 1L,
    val outfitId: Long,
    val userPhotoPath: String?,
    val generatedImagePath: String?,
    val previewImageRes: Int?,
    val harmonyScore: Int,
    val fitType: String,
    val pose: String,
    val lighting: String,
    val activePiecesCount: Int,
    val isSavedLook: Boolean = false,
    val garmentSummary: String? = null,
    val createdAt: Long
)

@Entity(tableName = "weekly_schedules")
data class WeeklyScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dayOfWeek: String,
    val dateDay: Int,
    val title: String,
    val weatherForecast: String,
    val formalityBadge: String,
    val outfitTitle: String,
    val outfitSubtitle: String,
    val outfitImageRes: Int?,
    val isToday: Boolean,
    val isWorn: Boolean,
    val isUnscheduled: Boolean,
    val dateIso: String = "",
    val outfitId: Long? = null,
    val occasion: String = "Everyday",
    val reminderEnabled: Boolean = false,
    val reminderTimeMinutes: Int = 480,
    val notes: String = "",
    val wornAtEpochMillis: Long? = null,
    val ownerUid: String = ""
)

@Entity(tableName = "wear_history")
data class WearHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val outfitId: Long,
    val outfitTitle: String,
    val clothingItemIds: String,
    val wornAtEpochMillis: Long,
    val occasion: String,
    val ownerUid: String = ""
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val remoteId: String? = null,
    val ownerUid: String = "",
    val name: String,
    val category: String,
    val estimatedPrice: Double = 0.0,
    val currency: String = "USD",
    val shoppingUrl: String = "",
    val reason: String = "",
    val priority: String = "MEDIUM",
    val status: String = "NEEDED",
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "outfit_feedback", indices = [androidx.room.Index(value = ["ownerUid", "outfitId"], unique = true)])
data class OutfitFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val feedbackId: Long = 0L,
    val ownerUid: String,
    val outfitId: Long,
    val rating: Int? = null,
    val reaction: String? = null,
    val comment: String = "",
    val outfitTitle: String,
    val occasion: String,
    val style: String,
    val colors: String,
    val categories: String,
    val itemNames: String,
    val createdAt: Long,
    val updatedAt: Long,
    val synced: Boolean = false,
    val deleted: Boolean = false
)
