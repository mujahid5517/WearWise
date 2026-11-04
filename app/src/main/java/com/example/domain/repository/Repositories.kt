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
import com.example.domain.model.ShoppingAnalysis
import com.example.domain.model.ShoppingChatMessage
import com.example.domain.model.ShoppingItem
import com.example.domain.model.ShoppingRecommendation
import com.example.domain.model.Trip
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun resetLearnedPreferences()
}

data class AuthenticatedUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean
)

data class AuthenticationState(
    val isReady: Boolean = false,
    val user: AuthenticatedUser? = null
)

interface AuthenticationRepository {
    val state: StateFlow<AuthenticationState>
    suspend fun register(fullName: String, email: String, password: String): AuthenticatedUser
    suspend fun signIn(email: String, password: String): AuthenticatedUser
    suspend fun sendPasswordReset(email: String)
    suspend fun updatePassword(currentPassword: String, newPassword: String)
    suspend fun updateProfile(profile: UserProfile)
    suspend fun updateProfilePhoto(localUri: String): String
    suspend fun deleteAccount(password: String)
    fun signOut()
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

interface ShoppingRepository {
    fun observeShoppingItems(): Flow<List<ShoppingItem>>
    suspend fun analyzeWardrobe(
        wardrobe: List<ClothingItem>,
        profile: UserProfile?,
        budget: Double,
        currency: String,
        occasion: String,
        weather: String?,
        focusCategory: String?
    ): ShoppingAnalysis
    suspend fun chat(
        question: String,
        history: List<ShoppingChatMessage>,
        wardrobe: List<ClothingItem>,
        profile: UserProfile?,
        budget: Double,
        currency: String,
        occasion: String,
        weather: String?,
        focusCategory: String?
    ): String
    suspend fun addItem(item: ShoppingItem): Long
    suspend fun updateItem(item: ShoppingItem)
    suspend fun deleteItem(id: Long)
}

interface TripRepository {
    fun observeAllTrips(): Flow<List<Trip>>
    suspend fun getTripById(id: Long): Trip?
    suspend fun createTrip(
        destination: String,
        departureDate: String,
        returnDate: String,
        weatherPreference: String,
        occasion: String,
        wardrobe: List<ClothingItem>,
        profile: UserProfile?
    ): Long
    suspend fun updateTrip(trip: Trip)
    suspend fun deleteTrip(id: Long)
}
