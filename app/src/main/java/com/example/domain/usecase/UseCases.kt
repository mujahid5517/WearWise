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
    val reason: String = "",
    val title: String = "",
    val occasion: String = "Everyday",
    val forecastTime: String = "",
    val date: String = "",
    val locationName: String = "",
    val isCached: Boolean = false
)

object WardrobeWeatherRecommender {
    fun recommend(
        conditions: WeatherForecast,
        clothes: List<ClothingItem>,
        history: List<com.example.domain.model.WearHistory>,
        variation: Int,
        occasion: String = "Everyday",
        forecastTime: String = "",
        date: String = ""
    ): WeatherOutfitSuggestion {
        val temperature = conditions.feelsLikeC ?: conditions.temperatureC
        val hot = temperature != null && temperature >= 29.0
        val cold = temperature != null && temperature < 12.0
        val rainy = (conditions.precipitationProbability ?: 0) >= 40 ||
            (conditions.precipitationMm ?: 0.0) >= 0.5 ||
            conditions.weatherCode?.let { it in RAIN_CODES } == true
        val windy = (conditions.windKmh ?: 0.0) >= 25.0
        val sunny = conditions.weatherCode?.let { it in SUN_CODES } == true
        val wornToday = history.filter {
            java.time.Instant.ofEpochMilli(it.wornAtEpochMillis)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString() == date
        }.flatMap { it.clothingItemIds }.toSet()
        val variationOffset = variation.coerceAtLeast(0)
        fun select(
            category: String,
            required: Boolean = false,
            preferredWords: List<String> = emptyList(),
            avoidedWords: List<String> = emptyList()
        ): ClothingItem? {
            val candidates = clothes.filter {
                it.category.equals(category, ignoreCase = true) ||
                    (category == "Tops" && it.category.equals("Knitwear", ignoreCase = true))
            }
                .sortedByDescending { item ->
                    val searchText = listOf(item.name, item.subcategory, item.description, item.fabric, item.season)
                        .joinToString(" ").lowercase()
                    preferredWords.count(searchText::contains) * 5 -
                        avoidedWords.count(searchText::contains) * 7 -
                        if (item.id in wornToday) 2 else 0
                }
            if (candidates.isEmpty() && required) return null
            return candidates.getOrNull(variationOffset % candidates.size.coerceAtLeast(1))
        }

        val onePiece = select(
            "Dresses and One-Piece",
            preferredWords = when {
                hot -> listOf("summer", "light", "linen", "cotton", "breathable")
                cold -> listOf("long", "wool", "knit", "warm")
                else -> emptyList()
            }
        )
        val top = if (onePiece == null) select(
            "Tops",
            required = true,
            preferredWords = when {
                hot -> listOf("summer", "light", "linen", "cotton", "breathable")
                cold -> listOf("sweater", "wool", "knit", "thermal", "warm")
                else -> emptyList()
            },
            avoidedWords = if (hot) listOf("wool", "thermal", "heavy") else emptyList()
        ) else null
        val bottom = if (onePiece == null) select(
            "Bottoms",
            required = true,
            preferredWords = if (hot) listOf("shorts", "light", "linen", "cotton") else emptyList(),
            avoidedWords = if (hot) listOf("wool", "thermal", "heavy") else emptyList()
        ) else null
        val shoes = select(
            "Shoes",
            required = true,
            preferredWords = when {
                rainy -> listOf("waterproof", "water-resistant", "rain", "boot", "rubber")
                hot -> listOf("sandal", "sneaker", "light", "comfortable")
                cold -> listOf("boot", "leather", "warm")
                else -> listOf("comfortable", "sneaker")
            },
            avoidedWords = when {
                rainy -> listOf("canvas", "suede", "mesh")
                hot -> listOf("heavy", "winter")
                else -> emptyList()
            }
        )
        val outerwear = when {
            cold -> select("Outerwear", preferredWords = listOf("coat", "jacket", "wool", "warm", "insulated"))
            rainy || windy -> select("Outerwear", preferredWords = listOf("rain", "waterproof", "windproof", "shell", "jacket"))
            else -> null
        }
        val weatherSafeShoes = shoes?.let { item ->
            listOf(item.name, item.subcategory, item.description, item.fabric)
                .joinToString(" ").containsAny("waterproof", "water-resistant", "rain", "boot", "rubber")
        } ?: false
        val weatherSafeOuterwear = outerwear?.let { item ->
            listOf(item.name, item.subcategory, item.description, item.fabric)
                .joinToString(" ").containsAny("waterproof", "water-resistant", "rain", "windproof", "shell")
        } ?: false

        val items = listOfNotNull(onePiece ?: top, bottom, outerwear, shoes)
            .distinctBy { it.id }
            .toMutableList()
        if (sunny) {
            select("Accessories", preferredWords = listOf("sunglasses", "sun hat", "cap"))
                ?.takeIf { listOf(it.name, it.subcategory, it.description).joinToString(" ").containsAny("sunglass", "sun hat", "cap") }
                ?.let(items::add)
        }
        val missing = when {
            clothes.isEmpty() -> "Your wardrobe is empty. Add clothing items to get a weather-ready outfit."
            onePiece == null && (top == null || bottom == null) ->
                "A complete outfit needs a top and bottom or a one-piece item. Add suitable clothing to your wardrobe."
            shoes == null -> "No footwear is available in your wardrobe. Add shoes to complete the outfit."
            cold && outerwear == null ->
                "It will feel cold, but your wardrobe has no outerwear. Add a warm jacket or coat for this forecast."
            rainy && !weatherSafeShoes && !weatherSafeOuterwear ->
                "Rain is expected, but no rain-appropriate shoes or outerwear were found in your wardrobe."
            else -> null
        }
        val timeText = forecastTime.ifBlank { "current conditions" }
        val temperatureText = temperature?.let { "${it.toInt()}°C feels-like" }
            ?: conditions.temperatureC?.let { "${it.toInt()}°C" }
            ?: "temperature unavailable"
        val conditionText = "${conditions.weatherCode?.let(WeatherDescriptions::describe) ?: "Weather unavailable"}, " +
            "$temperatureText, rain ${conditions.precipitationProbability?.let { "$it%" } ?: "probability unavailable"}"
        val reason = missing ?: buildString {
            append("For $occasion at $timeText: ")
            append(conditionText)
            if (hot) append(". Prioritized lightweight, breathable choices and skipped heavy outerwear")
            if (cold) append(". Included a warm layer because the feels-like temperature is cold")
            if (rainy) append(if (weatherSafeShoes || weatherSafeOuterwear) ". Prioritized rain-appropriate pieces" else ". Your wardrobe has limited rain protection")
            if (windy) append(". Included outerwear for the wind")
            if (sunny) append(". Added sun protection only if it exists in your wardrobe")
            append(". Every item is selected from your wardrobe.")
        }
        val title = when {
            missing != null -> "No suitable outfit yet"
            onePiece != null -> "${onePiece.name} · weather-ready"
            else -> listOfNotNull(top?.name, bottom?.name).joinToString(" + ").ifBlank { "Weather-ready outfit" }
        }
        return WeatherOutfitSuggestion(
            conditionSummary = conditionText,
            items = if (missing == null) items else emptyList(),
            reason = reason,
            title = title,
            occasion = occasion,
            forecastTime = timeText,
            date = date,
            locationName = conditions.location?.displayName.orEmpty(),
            isCached = conditions.isCached
        )
    }

    private fun String.containsAny(vararg values: String): Boolean =
        values.any { contains(it, ignoreCase = true) }

    private val RAIN_CODES = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)
    private val SUN_CODES = setOf(0, 1)
}
