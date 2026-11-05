package com.example.domain.model

data class UserProfile(
    val id: Long = 1L,
    val firstName: String = "Yared",
    val nickname: String = "",
    val email: String = "yared@institution.edu",
    val ageRange: String = "",
    val gender: String = "",
    val country: String = "United States",
    val region: String = "Cambridge, MA",
    val profession: String = "Academic & Research",
    val dressEnvironment: String = "University / Research",
    val stylePreferences: List<String> = listOf("Smart Casual"),
    val favoriteColors: List<String> = listOf("Deep Navy", "Charcoal"),
    val avoidedColors: List<String> = listOf("Neon Yellow"),
    val commonOccasions: List<String> = listOf("Work", "Everyday"),
    val climate: String = "Temperate",
    val silhouetteArchetype: String = "Athletic Lean",
    val profileImage: String? = null,
    val profileImagePath: String? = null,
    val profileImageRes: Int? = null,
    val colorSeason: String = "",
    val skinUndertone: String = "",
    val hairColorAnalyzed: String = "",
    val eyeColorAnalyzed: String = "",
    val recommendedColors: List<String> = emptyList(),
    val colorsToAvoid: List<String> = emptyList(),
    val colorAnalysisRationale: String = "",
    val colorAnalysisPhotoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ColorSwatch(
    val name: String,
    val hex: String
)

data class ColorAnalysisResult(
    val season: String,
    val undertone: String,
    val hairColor: String,
    val eyeColor: String,
    val recommendedColors: List<ColorSwatch>,
    val colorsToAvoid: List<ColorSwatch>,
    val rationale: String
)

data class ClothingItem(
    val id: Long = 0L,
    val userId: Long = 1L,
    val name: String,
    val category: String, // Tops, Bottoms, Outerwear, Shoes, Knitwear, Accessories
    val color: String,
    val style: String,
    val season: String = "All-Season",
    val formality: String = "Smart Casual",
    val formalityScore: Int = 7, // e.g. 7/10
    val fabric: String = "100% Cotton",
    val isFavorite: Boolean = false,
    val imageRes: Int? = null,
    val imageUri: String? = null,
    val wearCount: Int = 0,
    val lastWornDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val subcategory: String = "",
    val brand: String = "",
    val size: String = "",
    val occasion: String = "",
    val description: String = "",
    val remoteId: String? = null,
    val imageStoragePath: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val pattern: String = "",
    val ownerUid: String = "",
    val catalogAssetName: String? = null
)

object WardrobeTaxonomy {
    val categories = linkedMapOf(
        "Tops" to listOf("T-shirts", "Shirts", "Sweaters", "Hoodies", "Blouses"),
        "Bottoms" to listOf("Jeans", "Trousers", "Shorts", "Skirts"),
        "Outerwear" to listOf("Jackets", "Coats", "Blazers"),
        "Dresses and One-Piece" to listOf("Dresses", "Jumpsuits"),
        "Shoes" to listOf("Sneakers", "Formal shoes", "Boots", "Sandals"),
        "Accessories" to listOf("Watches", "Bags", "Belts", "Hats", "Sunglasses")
    )
    val seasons = listOf("All Season", "Spring", "Summer", "Autumn", "Winter")
    val occasions = listOf("Everyday", "Work", "Formal", "Party", "Sport", "Travel")
}

data class Outfit(
    val id: Long = 0L,
    val userId: Long = 1L,
    val title: String,
    val occasion: String,
    val style: String,
    val score: Int = 98, // Harmony percentage
    val rationale: String,
    val weatherTag: String = "Sunny 21°C • University Campus",
    val isFavorite: Boolean = false,
    val items: List<ClothingItem> = emptyList(),
    val previewImageRes: Int? = null,
    val previewImageUri: String? = null,
    val occasionFitScore: Int = 96,
    val thermalSyncScore: Int = 99,
    val colorHarmonyScore: Int = 98,
    val createdAt: Long = System.currentTimeMillis(),
    val ownerUid: String = "",
    val colorCompatibilityExplanation: String = "",
    val occasionSuitability: String = "",
    val isAiGenerated: Boolean = false
)

/** Stable Room key for a transient recommendation that has not been saved as an outfit yet. */
fun Outfit.previewCacheKey(): Long {
    val signature = if (id > 0L) "saved:$id" else
        items.map { it.id }.filter { it > 0L }.sorted().joinToString("-").ifBlank { title }
    return "$ownerUid:$signature".hashCode().toLong().and(Long.MAX_VALUE).coerceAtLeast(1L)
}

data class TryOnPreview(
    val id: Long = 0L,
    val userId: Long = 1L,
    val outfitId: Long,
    val userPhotoPath: String? = null,
    val generatedImagePath: String? = null,
    val previewImageRes: Int? = null,
    val harmonyScore: Int = 94,
    val fitType: String = "Tailored", // Relaxed, Tailored, Slim
    val pose: String = "Confident Stance",
    val lighting: String = "Warm Daylight", // Warm Daylight, Softbox, Lounge
    val activePiecesCount: Int = 4,
    val isSavedLook: Boolean = false,
    val garmentSummary: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ShoppingItem(
    val id: Long = 0L,
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

data class ShoppingRecommendation(
    val name: String,
    val category: String,
    val priority: String,
    val why: String,
    val suggestedColor: String,
    val suggestedStyle: String,
    val budgetTarget: Double,
    val compatibleItems: List<String>,
    val outfitCombinationCount: Int,
    val affordableAlternatives: List<String> = emptyList()
)

data class ShoppingAnalysis(
    val summary: String,
    val duplicateNotes: List<String>,
    val recommendations: List<ShoppingRecommendation>
)

data class ShoppingChatMessage(val text: String, val isUser: Boolean)

data class SelectedTryOnGarment(
    val slot: String,
    val name: String,
    val category: String,
    val imageRes: Int? = null,
    val imageUri: String? = null,
    val clothingItemId: Long? = null,
    val imageUrl: String? = null,
    val catalogAssetName: String? = null
)

data class WeeklyScheduleItem(
    val id: Long = 0L,
    val dayOfWeek: String, // MON, TUE, WED, THU, FRI, SAT, SUN
    val dateDay: Int, // e.g. 14, 15, 16...
    val title: String,
    val weatherForecast: String,
    val formalityBadge: String,
    val outfitTitle: String,
    val outfitSubtitle: String,
    val outfitImageRes: Int? = null,
    val isToday: Boolean = false,
    val isWorn: Boolean = false,
    val isUnscheduled: Boolean = false,
    val dateIso: String = "",
    val outfitId: Long? = null,
    val occasion: String = "Everyday",
    val reminderEnabled: Boolean = false,
    val reminderTimeMinutes: Int = 480,
    val notes: String = "",
    val wornAtEpochMillis: Long? = null,
    val ownerUid: String = ""
)

data class WearHistory(
    val id: Long = 0L,
    val outfitId: Long = 0L,
    val outfitTitle: String = "",
    val clothingItemIds: List<Long> = emptyList(),
    val wornAtEpochMillis: Long = System.currentTimeMillis(),
    val occasion: String = "",
    val ownerUid: String = ""
)

data class WeatherLocation(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val name: String = displayName,
    val admin1: String = "",
    val country: String = ""
)

data class DailyWeather(
    val date: String,
    val highC: Double? = null,
    val lowC: Double? = null,
    val apparentHighC: Double? = null,
    val precipitationProbability: Int? = null,
    val maxWindKmh: Double? = null,
    val weatherCode: Int? = null
)

data class HourlyWeather(
    val time: String,
    val weatherCode: Int? = null,
    val temperatureC: Double? = null,
    val precipitationProbability: Int? = null
)

data class WeatherForecast(
    val location: WeatherLocation? = null,
    val temperatureC: Double? = null,
    val feelsLikeC: Double? = null,
    val precipitationProbability: Int? = null,
    val precipitationMm: Double? = null,
    val windKmh: Double? = null,
    val weatherCode: Int? = null,
    val humidityPercent: Int? = null,
    val fetchedAtEpochMillis: Long = System.currentTimeMillis(),
    val daily: List<DailyWeather> = emptyList(),
    val hourly: List<HourlyWeather> = emptyList()
)

data class TripPackingItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: String,
    val itemName: String,
    val isPacked: Boolean = false,
    val clothingItemId: Long? = null
)

data class TripDayOutfit(
    val dayNumber: Int,
    val dateStr: String,
    val outfitTitle: String,
    val rationale: String,
    val clothingItemNames: List<String>
)

data class Trip(
    val id: Long = 0L,
    val destination: String,
    val departureDate: String,
    val returnDate: String,
    val weatherPreference: String,
    val occasion: String,
    val packingList: List<TripPackingItem> = emptyList(),
    val itinerary: List<TripDayOutfit> = emptyList(),
    val ownerUid: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    val repackingAlert: String = "",
    val originLat: Double? = null,
    val originLng: Double? = null,
    val travelDistanceKm: Double? = null,
    val travelDurationHours: Double? = null,
    val transportMode: String = "Car"
)

object WeatherDescriptions {
    fun describe(code: Int?): String {
    val outfitId: Long,
    val outfitTitle: String,
    val clothingItemIds: List<Long>,
    val wornAtEpochMillis: Long,
    val occasion: String
)

data class WeatherHourlyForecast(
    val time: String = "",
    val weatherCode: Int = 0,
    val temperatureC: Int = 20,
    val precipitationProbability: Int? = 10
)

data class WeatherDailyForecast(
    val date: String = "",
    val weatherCode: Int? = 0,
    val highC: Int? = 20,
    val lowC: Int? = 12,
    val apparentHighC: Int? = 20,
    val precipitationProbability: Int? = 10,
    val maxWindKmh: Int? = 10
)

data class WeatherForecast(
    val location: WeatherLocation = WeatherLocation(),
    val temperatureC: Int? = 20,
    val feelsLikeC: Int? = 20,
    val weatherCode: Int = 0,
    val precipitationProbability: Int? = 10,
    val precipitationMm: Double = 0.0,
    val humidityPercent: Int? = 50,
    val windKmh: Int? = 10,
    val fetchedAtEpochMillis: Long = System.currentTimeMillis(),
    val daily: List<WeatherDailyForecast> = emptyList(),
    val hourly: List<WeatherHourlyForecast> = emptyList()
)

data class WeatherLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val name: String = "Current Location"
) {
    val displayName: String get() = name
}

object WeatherDescriptions {
    fun describe(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Foggy"
            51, 53, 55, 56, 57 -> "Drizzle"
            61, 63, 65, 66, 67 -> "Rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Rain showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Fair"
        }
    }
}
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            80, 81, 82 -> "Rain showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Partly cloudy"
        }
    }
}

sealed class TryOnGenerationException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidImage(message: String) : TryOnGenerationException(message)
    class NoImageReturned(message: String) : TryOnGenerationException(message)
    class ApiError(message: String, cause: Throwable? = null) : TryOnGenerationException(message, cause)
}
