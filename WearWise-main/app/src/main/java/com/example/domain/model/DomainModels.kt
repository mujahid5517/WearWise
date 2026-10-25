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
    val createdAt: Long = System.currentTimeMillis()
)

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
    val createdAt: Long = System.currentTimeMillis()
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
    val createdAt: Long = System.currentTimeMillis()
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
    val isUnscheduled: Boolean = false
)
