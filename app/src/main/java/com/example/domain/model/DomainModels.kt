package com.example.domain.model

data class UserProfile(
    val id: Long = 1L,
    val firstName: String = "Yared",
    val nickname: String = "Dr. Yared",
    val email: String = "yared@institution.edu",
    val ageRange: String = "25-34",
    val gender: String = "Male",
    val country: String = "United States",
    val region: String = "Cambridge, MA",
    val profession: String = "Lecturer & Researcher",
    val dressEnvironment: String = "Smart Casual to Business Casual",
    val stylePreferences: List<String> = listOf("Smart Casual", "Minimalist", "Classic", "Modern Tech"),
    val favoriteColors: List<String> = listOf("Deep Navy", "Charcoal Grey", "Crisp White", "Olive Green", "Camel / Tan", "Black / Noir"),
    val avoidedColors: List<String> = listOf("Neon Yellow", "Bright Hot Pink"),
    val commonOccasions: List<String> = listOf("Lectures & Teaching", "Research Lab", "Faculty Meetings", "Weekend Outings", "Conferences"),
    val climate: String = "Hyper-local weather sync (58°F / 14°C)",
    val silhouetteArchetype: String = "Athletic Lean (40\" Chest, 31\" Waist, 183cm)",
    val profileImage: String? = null,
    val profileImageRes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
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
    val ownerUid: String = ""
)

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
