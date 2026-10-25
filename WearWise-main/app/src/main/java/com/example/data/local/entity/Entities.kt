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
    val createdAt: Long
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
    val createdAt: Long
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
    val isUnscheduled: Boolean
)
