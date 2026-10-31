package com.example.domain.repository

import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.domain.model.SelectedTryOnGarment
import com.example.domain.model.TryOnPreview
import com.example.domain.model.UserProfile
import com.example.domain.model.WeeklyScheduleItem
import com.example.domain.model.WeatherForecast
import com.example.domain.model.WeatherLocation
import com.example.domain.model.WearHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun resetLearnedPreferences()
}

interface WardrobeRepository {
    val syncMessage: StateFlow<String?>
    val uploadProgress: StateFlow<Float?>
    fun getAllClothingItems(): Flow<List<ClothingItem>>
    fun getClothingItemsByCategory(category: String): Flow<List<ClothingItem>>
    suspend fun addClothingItem(item: ClothingItem): Long
    suspend fun updateClothingItem(item: ClothingItem)
    suspend fun deleteClothingItem(id: Long)
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun persistClothingImage(uri: String): String
}

interface OutfitRepository {
    fun getAllOutfits(): Flow<List<Outfit>>
    fun getFavoriteOutfits(): Flow<List<Outfit>>
    suspend fun getDailyRecommendation(): Outfit
    suspend fun generateAiRecommendation(): Outfit?
    suspend fun generateWeatherRecommendation(weather: WeatherForecast, occasion: String): Outfit?
    suspend fun saveOutfit(outfit: Outfit): Long
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun deleteOutfit(id: Long)
    fun getWeeklySchedule(): Flow<List<WeeklyScheduleItem>>
    suspend fun saveScheduleItem(item: WeeklyScheduleItem): Long
    suspend fun updateScheduleItem(item: WeeklyScheduleItem)
    suspend fun deleteScheduleItem(id: Long)
    suspend fun regenerateWeeklySchedule()
    suspend fun recordOutfitWorn(outfitId: Long)
    fun getWearHistory(): Flow<List<WearHistory>>
}

interface WeatherRepository {
    suspend fun refresh(location: WeatherLocation, force: Boolean = false): WeatherForecast
    suspend fun cachedForecast(): WeatherForecast?
    suspend fun searchLocations(query: String): List<WeatherLocation>
    fun getSavedLocation(): WeatherLocation?
    fun saveLocation(location: WeatherLocation)
}

interface TryOnRepository {
    fun getAllTryOnPreviews(): Flow<List<TryOnPreview>>
    suspend fun deleteTryOnPreview(id: Long)
    suspend fun setSavedLook(id: Long, saved: Boolean)
    suspend fun persistPersonPhoto(uri: String): String
    suspend fun persistGarmentPhoto(uri: String): String
    suspend fun persistPersonBitmap(bitmap: android.graphics.Bitmap): String
    suspend fun adjustPersonPhoto(path: String, rotationDegrees: Int): String
    suspend fun rotatePersonPhoto(path: String, rotationDegrees: Int): String
    suspend fun cropPersonPhoto(path: String, left: Float, top: Float, right: Float, bottom: Float): String
    suspend fun saveGeneratedToGallery(path: String): android.net.Uri
    fun shareUriForGenerated(path: String): android.net.Uri
    suspend fun generateTryOn(
        outfitId: Long,
        fitType: String,
        pose: String,
        lighting: String,
        userPhotoUri: String,
        garments: List<SelectedTryOnGarment>
    ): TryOnPreview
}
