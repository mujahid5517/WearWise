package com.example.domain.repository

import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.domain.model.TryOnPreview
import com.example.domain.model.UserProfile
import com.example.domain.model.WeeklyScheduleItem
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun resetLearnedPreferences()
}

interface WardrobeRepository {
    fun getAllClothingItems(): Flow<List<ClothingItem>>
    fun getClothingItemsByCategory(category: String): Flow<List<ClothingItem>>
    suspend fun addClothingItem(item: ClothingItem): Long
    suspend fun updateClothingItem(item: ClothingItem)
    suspend fun deleteClothingItem(id: Long)
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
}

interface OutfitRepository {
    fun getAllOutfits(): Flow<List<Outfit>>
    fun getFavoriteOutfits(): Flow<List<Outfit>>
    suspend fun getDailyRecommendation(): Outfit
    suspend fun saveOutfit(outfit: Outfit): Long
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    fun getWeeklySchedule(): Flow<List<WeeklyScheduleItem>>
    suspend fun regenerateWeeklySchedule()
}

interface TryOnRepository {
    fun getAllTryOnPreviews(): Flow<List<TryOnPreview>>
    suspend fun simulateTryOn(
        outfitId: Long,
        fitType: String,
        pose: String,
        lighting: String,
        userPhotoUri: String?,
        garmentDescriptions: List<String> = emptyList()
    ): TryOnPreview
}
