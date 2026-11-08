package com.example.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.domain.model.SelectedTryOnGarment
import com.example.domain.model.TryOnPreview
import com.example.domain.model.UserProfile
import com.example.domain.model.WeeklyScheduleItem
import com.example.domain.repository.OutfitRepository
import com.example.domain.repository.TryOnRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WardrobeRepository
import com.example.domain.repository.WeatherRepository
import com.example.domain.model.WeatherLocation
import com.example.domain.model.WeatherForecast
import com.example.domain.model.WeatherDescriptions
import kotlinx.coroutines.flow.Flow

class GetDailyRecommendationUseCase(
    private val outfitRepository: OutfitRepository
) {
    suspend operator fun invoke(): Outfit {
        return outfitRepository.getDailyRecommendation()
    }

    suspend fun generateAiRecommendation(): Outfit? = outfitRepository.generateAiRecommendation()
}

class ManageWardrobeUseCase(
    private val wardrobeRepository: WardrobeRepository
) {
    val syncMessage = wardrobeRepository.syncMessage
    val uploadProgress = wardrobeRepository.uploadProgress
    fun getAllItems(): Flow<List<ClothingItem>> = wardrobeRepository.getAllClothingItems()

    fun getItemsByCategory(category: String): Flow<List<ClothingItem>> =
        wardrobeRepository.getClothingItemsByCategory(category)

    suspend fun addItem(item: ClothingItem): Long = wardrobeRepository.addClothingItem(item)

    suspend fun updateItem(item: ClothingItem) = wardrobeRepository.updateClothingItem(item)

    suspend fun deleteItem(id: Long) = wardrobeRepository.deleteClothingItem(id)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        wardrobeRepository.toggleFavorite(id, isFavorite)

    suspend fun persistClothingImage(uri: String): String = wardrobeRepository.persistClothingImage(uri)
}

class ExecuteTryOnUseCase(
    private val tryOnRepository: TryOnRepository
) {
    fun getHistory(): Flow<List<TryOnPreview>> = tryOnRepository.getAllTryOnPreviews()

    suspend fun getCachedOutfitPreview(outfitKey: Long): String? =
        tryOnRepository.getCachedOutfitPreview(outfitKey)

    suspend fun generateOutfitPreview(outfit: Outfit, modelImageRes: Int): String =
        tryOnRepository.generateOutfitPreview(outfit, modelImageRes)

    suspend fun deleteHistoryEntry(id: Long) = tryOnRepository.deleteTryOnPreview(id)

    suspend fun setSavedLook(id: Long, saved: Boolean) = tryOnRepository.setSavedLook(id, saved)

    suspend fun persistPersonPhoto(uri: String): String = tryOnRepository.persistPersonPhoto(uri)

    suspend fun persistGarmentPhoto(uri: String): String = tryOnRepository.persistGarmentPhoto(uri)

    suspend fun persistPersonBitmap(bitmap: Bitmap): String = tryOnRepository.persistPersonBitmap(bitmap)

    suspend fun adjustPersonPhoto(path: String, rotationDegrees: Int): String =
        tryOnRepository.adjustPersonPhoto(path, rotationDegrees)

    suspend fun rotatePersonPhoto(path: String, rotationDegrees: Int): String =
        tryOnRepository.rotatePersonPhoto(path, rotationDegrees)

    suspend fun cropPersonPhoto(
        path: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): String = tryOnRepository.cropPersonPhoto(path, left, top, right, bottom)

    suspend fun saveGeneratedToGallery(path: String): Uri = tryOnRepository.saveGeneratedToGallery(path)

    fun shareUriForGenerated(path: String): Uri = tryOnRepository.shareUriForGenerated(path)

    suspend fun execute(
        outfitId: Long,
        fitType: String,
        pose: String,
        lighting: String,
        userPhotoUri: String,
        garments: List<SelectedTryOnGarment>
    ): TryOnPreview {
        return tryOnRepository.generateTryOn(
            outfitId,
            fitType,
            pose,
            lighting,
            userPhotoUri,
            garments
        )
    }
}

class WeeklyPlannerUseCase(
    private val outfitRepository: OutfitRepository
) {
    fun getSchedule(): Flow<List<WeeklyScheduleItem>> = outfitRepository.getWeeklySchedule()

    suspend fun regenerateSchedule() = outfitRepository.regenerateWeeklySchedule()

    suspend fun updateScheduleItem(item: WeeklyScheduleItem) =
        outfitRepository.updateScheduleItem(item)

    suspend fun saveScheduleItem(item: WeeklyScheduleItem): Long = outfitRepository.saveScheduleItem(item)

    suspend fun deleteScheduleItem(id: Long) = outfitRepository.deleteScheduleItem(id)
}

class GetWeatherUseCase(private val weatherRepository: WeatherRepository) {
    suspend fun refresh(location: WeatherLocation, force: Boolean = false) = weatherRepository.refresh(location, force)
    suspend fun cached() = weatherRepository.cachedForecast()
    suspend fun search(query: String) = weatherRepository.searchLocations(query)
    fun savedLocation() = weatherRepository.getSavedLocation()
    fun saveLocation(location: WeatherLocation) = weatherRepository.saveLocation(location)
}

class UserProfileUseCase(
    private val userRepository: UserRepository
) {
    fun getProfile(): Flow<UserProfile?> = userRepository.getUserProfile()

    suspend fun saveProfile(profile: UserProfile) = userRepository.saveUserProfile(profile)

    suspend fun resetPreferences() = userRepository.resetLearnedPreferences()
}

data class WeatherOutfitSuggestion(
    val conditionSummary: String = "",
    val items: List<ClothingItem> = emptyList(),
    val reason: String = ""
)

object WardrobeWeatherRecommender {
    fun recommend(
        conditions: WeatherForecast,
        clothes: List<ClothingItem>,
        history: List<com.example.domain.model.WearHistory>,
        variation: Int
    ): WeatherOutfitSuggestion {
        val matchedItems = listOfNotNull(
            clothes.firstOrNull { it.category.equals("Tops", true) },
            clothes.firstOrNull { it.category.equals("Bottoms", true) },
            clothes.firstOrNull { it.category.equals("Shoes", true) }
        )
        val items = matchedItems.ifEmpty { clothes.shuffled().take(3) }
        return WeatherOutfitSuggestion(
            conditionSummary = "${conditions.temperatureC?.toInt() ?: 20}°C, ${WeatherDescriptions.describe(conditions.weatherCode)}",
            items = items,
            reason = "Weather-optimized selection based on current forecast."
        )
    }
}
