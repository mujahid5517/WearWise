package com.example.domain.usecase

import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.domain.model.TryOnPreview
import com.example.domain.model.UserProfile
import com.example.domain.model.WeeklyScheduleItem
import com.example.domain.repository.OutfitRepository
import com.example.domain.repository.TryOnRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WardrobeRepository
import kotlinx.coroutines.flow.Flow

class GetDailyRecommendationUseCase(
    private val outfitRepository: OutfitRepository
) {
    suspend operator fun invoke(): Outfit {
        return outfitRepository.getDailyRecommendation()
    }
}

class ManageWardrobeUseCase(
    private val wardrobeRepository: WardrobeRepository
) {
    fun getAllItems(): Flow<List<ClothingItem>> = wardrobeRepository.getAllClothingItems()

    fun getItemsByCategory(category: String): Flow<List<ClothingItem>> =
        wardrobeRepository.getClothingItemsByCategory(category)

    suspend fun addItem(item: ClothingItem): Long = wardrobeRepository.addClothingItem(item)

    suspend fun updateItem(item: ClothingItem) = wardrobeRepository.updateClothingItem(item)

    suspend fun deleteItem(id: Long) = wardrobeRepository.deleteClothingItem(id)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        wardrobeRepository.toggleFavorite(id, isFavorite)
}

class ExecuteTryOnUseCase(
    private val tryOnRepository: TryOnRepository
) {
    fun getHistory(): Flow<List<TryOnPreview>> = tryOnRepository.getAllTryOnPreviews()

    suspend fun execute(
        outfitId: Long,
        fitType: String,
        pose: String,
        lighting: String,
        userPhotoUri: String? = null,
        garmentDescriptions: List<String> = emptyList()
    ): TryOnPreview {
        return tryOnRepository.simulateTryOn(outfitId, fitType, pose, lighting, userPhotoUri, garmentDescriptions)
    }
}

class WeeklyPlannerUseCase(
    private val outfitRepository: OutfitRepository
) {
    fun getSchedule(): Flow<List<WeeklyScheduleItem>> = outfitRepository.getWeeklySchedule()

    suspend fun regenerateSchedule() = outfitRepository.regenerateWeeklySchedule()
}

class UserProfileUseCase(
    private val userRepository: UserRepository
) {
    fun getProfile(): Flow<UserProfile?> = userRepository.getUserProfile()

    suspend fun saveProfile(profile: UserProfile) = userRepository.saveUserProfile(profile)

    suspend fun resetPreferences() = userRepository.resetLearnedPreferences()
}
