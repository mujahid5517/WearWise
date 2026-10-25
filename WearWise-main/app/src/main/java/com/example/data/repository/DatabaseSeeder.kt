package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.ClothingItemEntity
import com.example.data.local.entity.OutfitEntity
import com.example.data.local.entity.OutfitItemEntity
import com.example.data.local.entity.TryOnPreviewEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WeeklyScheduleEntity
import com.example.presentation.common.WearWiseDrawables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseSeeder {
    suspend fun seedIfNeeded(database: AppDatabase) = withContext(Dispatchers.IO) {
        val clothingCount = database.clothingItemDao().countItems()
        if (clothingCount == 0) {
            seedInitialData(database)
        } else if (clothingCount < 12) {
            seedStreetwearExpansion(database)
        }
    }

    private suspend fun seedInitialData(database: AppDatabase) {
        // 1. Initial User Profile
        val userProfile = UserProfileEntity(
            id = 1L,
            firstName = "Yared",
            nickname = "Dr. Yared",
            email = "yared@institution.edu",
            ageRange = "25-34",
            gender = "Male",
            country = "United States",
            region = "Cambridge, MA",
            profession = "Lecturer & Researcher",
            dressEnvironment = "Smart Casual to Business Casual",
            stylePreferences = "Smart Casual,Minimalist,Classic,Modern Tech",
            favoriteColors = "Deep Navy,Charcoal Grey,Crisp White,Olive Green,Camel / Tan,Black / Noir",
            avoidedColors = "Neon Yellow,Bright Hot Pink",
            commonOccasions = "Lectures & Teaching,Research Lab,Faculty Meetings,Weekend Outings,Conferences",
            climate = "Cambridge Synced (58°F • 14°C)",
            silhouetteArchetype = "Athletic Lean (40\" Chest, 31\" Waist, 183cm)",
            profileImage = null,
            profileImageRes = WearWiseDrawables.UserAvatar,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.userProfileDao().insertOrUpdateProfile(userProfile)

        // 2. Initial Digital Wardrobe (Google Stitch Academic Capsule)
        val items = listOf(
            ClothingItemEntity(
                name = "White Oxford Cotton",
                category = "Tops",
                color = "Crisp White",
                style = "Smart Casual",
                season = "All-Season",
                formality = "Smart Casual",
                formalityScore = 7,
                fabric = "100% Breathable Cotton",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemOxford,
                imageUri = null,
                wearCount = 14,
                lastWornDate = "2 days ago",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Navy Tailored Chinos",
                category = "Bottoms",
                color = "Deep Navy",
                style = "Minimalist",
                season = "Spring / Autumn",
                formality = "Smart Casual",
                formalityScore = 6,
                fabric = "98% Stretch Twill",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemChinos,
                imageUri = null,
                wearCount = 20,
                lastWornDate = "Yesterday",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Olive Knit Blazer",
                category = "Outerwear",
                color = "Olive Green",
                style = "Academic",
                season = "Winter / Fall",
                formality = "Business Casual",
                formalityScore = 8,
                fabric = "Unlined Boiled Wool",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemBlazer,
                imageUri = null,
                wearCount = 9,
                lastWornDate = "4 days ago",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Burnished Loafers",
                category = "Shoes",
                color = "Cognac Brown",
                style = "Classic",
                season = "All-Season",
                formality = "Elevated",
                formalityScore = 8,
                fabric = "Handcrafted Italian Calfskin",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemLoafers,
                imageUri = null,
                wearCount = 18,
                lastWornDate = "Yesterday",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Charcoal Merino Knit",
                category = "Knitwear",
                color = "Charcoal Grey",
                style = "Modern Min",
                season = "Winter / Fall",
                formality = "Smart Casual",
                formalityScore = 7,
                fabric = "100% Merino Wool",
                isFavorite = false,
                imageRes = WearWiseDrawables.ItemMerino,
                imageUri = null,
                wearCount = 11,
                lastWornDate = "5 days ago",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Minimalist Chrono",
                category = "Accessories",
                color = "Silver / Bauhaus Dial",
                style = "Luxury",
                season = "Timeless",
                formality = "Tier 1",
                formalityScore = 9,
                fabric = "Sapphire Glass & Steel",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemWatch,
                imageUri = null,
                wearCount = 35,
                lastWornDate = "Today",
                createdAt = System.currentTimeMillis()
            ),
            // Relaxed Streetwear & Athletic Luxury Expansion
            ClothingItemEntity(
                name = "Heavyweight Boxy Tee",
                category = "Tops",
                color = "Matte Noir",
                style = "Streetwear Minimalist",
                season = "All-Season",
                formality = "Casual",
                formalityScore = 4,
                fabric = "280 GSM Combed Cotton",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemStreetTee,
                imageUri = null,
                wearCount = 8,
                lastWornDate = "3 days ago",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Pleated Cargo Trousers",
                category = "Bottoms",
                color = "Olive Drab",
                style = "Athletic Luxury",
                season = "All-Season",
                formality = "Elevated Casual",
                formalityScore = 5,
                fabric = "Technical Washed Twill",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemCargoPants,
                imageUri = null,
                wearCount = 12,
                lastWornDate = "Yesterday",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Technical Bomber Jacket",
                category = "Outerwear",
                color = "Matte Charcoal",
                style = "Athletic Luxury",
                season = "Fall / Winter",
                formality = "Smart Casual",
                formalityScore = 7,
                fabric = "Storm-Resistant Stretch Nylon",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemTechBomber,
                imageUri = null,
                wearCount = 6,
                lastWornDate = "Last week",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Minimal Leather Low-Tops",
                category = "Shoes",
                color = "Triple White",
                style = "Clean Luxury",
                season = "All-Season",
                formality = "Smart Casual",
                formalityScore = 6,
                fabric = "Italian Nappa Leather",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemCourtSneakers,
                imageUri = null,
                wearCount = 22,
                lastWornDate = "Today",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Waffle-Knit Mockneck",
                category = "Knitwear",
                color = "Sand Beige",
                style = "Cozy Minimalist",
                season = "Fall / Winter",
                formality = "Smart Casual",
                formalityScore = 6,
                fabric = "100% Organic Waffle Cotton",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemWaffleKnit,
                imageUri = null,
                wearCount = 14,
                lastWornDate = "4 days ago",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Tactical Crossbody Sling",
                category = "Accessories",
                color = "Matte Noir",
                style = "Technical Utility",
                season = "All-Season",
                formality = "Casual",
                formalityScore = 5,
                fabric = "Technical Cordura & Gunmetal",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemSlingBag,
                imageUri = null,
                wearCount = 19,
                lastWornDate = "Yesterday",
                createdAt = System.currentTimeMillis()
            )
        )

        val insertedIds = items.map { database.clothingItemDao().insertClothingItem(it) }

        // 3. Initial Curated Outfit (Look #01 The Academic Tailor)
        val outfit = OutfitEntity(
            title = "The Academic Tailor",
            occasion = "Smart Academic",
            style = "Smart Casual",
            score = 98,
            rationale = "Tailored specifically for your lecturer schedule. The 100% Oxford cotton weave ensures all-day breathability during the Faculty Meeting, while the structured navy chinos project relaxed academic authority at the 2:00 PM Colloquium.",
            weatherTag = "Sunny 21°C • University Campus",
            isFavorite = true,
            previewImageRes = WearWiseDrawables.AcademicLook,
            previewImageUri = null,
            occasionFitScore = 96,
            thermalSyncScore = 99,
            colorHarmonyScore = 98,
            createdAt = System.currentTimeMillis()
        )
        val outfitId = database.outfitDao().insertOutfit(outfit)
        val outfitItems = insertedIds.map { OutfitItemEntity(outfitId = outfitId, clothingId = it) }
        database.outfitDao().insertOutfitItems(outfitItems)

        // 4. Initial Virtual Try-On Preview
        val preview = TryOnPreviewEntity(
            outfitId = outfitId,
            userPhotoPath = null,
            generatedImagePath = null,
            previewImageRes = WearWiseDrawables.TryOnModel,
            harmonyScore = 94,
            fitType = "Tailored",
            pose = "Confident Stance",
            lighting = "Warm Daylight",
            activePiecesCount = 4,
            createdAt = System.currentTimeMillis()
        )
        database.tryOnPreviewDao().insertTryOnPreview(preview)

        // 5. Initial Weekly Planner Schedule (October 14-20)
        val schedules = listOf(
            WeeklyScheduleEntity(
                dayOfWeek = "MON",
                dateDay = 14,
                title = "Faculty Welcome & Dean Address",
                weatherForecast = "19°C Part-Cloudy",
                formalityBadge = "Tier 3 • Formal",
                outfitTitle = "White Oxford + Olive Blazer + Chinos",
                outfitSubtitle = "Archived to Closet History",
                outfitImageRes = WearWiseDrawables.AcademicLook,
                isToday = false,
                isWorn = true,
                isUnscheduled = false
            ),
            WeeklyScheduleEntity(
                dayOfWeek = "TUE",
                dateDay = 15,
                title = "Research Lab & Colloquium",
                weatherForecast = "21°C Crisp & Clear",
                formalityBadge = "Smart Casual (82%)",
                outfitTitle = "Charcoal Merino + Slate Trousers",
                outfitSubtitle = "Accented with Polished Chelsea Boots",
                outfitImageRes = WearWiseDrawables.TryOnModel,
                isToday = true,
                isWorn = false,
                isUnscheduled = false
            ),
            WeeklyScheduleEntity(
                dayOfWeek = "WED",
                dateDay = 16,
                title = "Department Meeting & Dinner",
                weatherForecast = "18°C Mild Rain Predicted",
                formalityBadge = "High Formality",
                outfitTitle = "Double-breasted Navy Blazer + Crisp Shirt",
                outfitSubtitle = "Oxford Brogues with Dainite Soles",
                outfitImageRes = WearWiseDrawables.AcademicLook,
                isToday = false,
                isWorn = false,
                isUnscheduled = false
            ),
            WeeklyScheduleEntity(
                dayOfWeek = "THU",
                dateDay = 17,
                title = "Office Hours & Mentoring",
                weatherForecast = "22°C Sunny & Pleasant",
                formalityBadge = "Cozy Score 9.2",
                outfitTitle = "Knitted Polo + Beige Chinos",
                outfitSubtitle = "Suede Loafers + Leather Briefcase",
                outfitImageRes = WearWiseDrawables.TryOnModel,
                isToday = false,
                isWorn = false,
                isUnscheduled = false
            ),
            WeeklyScheduleEntity(
                dayOfWeek = "FRI",
                dateDay = 18,
                title = "Casual Friday & Social Mixer",
                weatherForecast = "20°C Breezy Evening",
                formalityBadge = "Elevated Casual",
                outfitTitle = "Indigo Denim + Breton Stripe",
                outfitSubtitle = "Clean White Sneakers + Wool Beanie",
                outfitImageRes = WearWiseDrawables.AcademicLook,
                isToday = false,
                isWorn = false,
                isUnscheduled = false
            ),
            WeeklyScheduleEntity(
                dayOfWeek = "SAT",
                dateDay = 19,
                title = "Weekend Getaway: Coastal Transit",
                weatherForecast = "Coastal 17°C",
                formalityBadge = "Unscheduled",
                outfitTitle = "Highland Transit Capsule",
                outfitSubtitle = "Rugged Layers & Travel Duffel",
                outfitImageRes = WearWiseDrawables.AcademicLook,
                isToday = false,
                isWorn = false,
                isUnscheduled = true
            ),
            WeeklyScheduleEntity(
                dayOfWeek = "SUN",
                dateDay = 20,
                title = "Brunch & Academic Prep",
                weatherForecast = "Brisk 15°C",
                formalityBadge = "Unscheduled",
                outfitTitle = "Brunch & Return Capsule",
                outfitSubtitle = "Relaxed Knit & Chinos",
                outfitImageRes = WearWiseDrawables.TryOnModel,
                isToday = false,
                isWorn = false,
                isUnscheduled = true
            )
        )
        database.weeklyScheduleDao().insertAll(schedules)
    }

    private suspend fun seedStreetwearExpansion(database: AppDatabase) {
        val streetwearItems = listOf(
            ClothingItemEntity(
                name = "Heavyweight Boxy Tee",
                category = "Tops",
                color = "Matte Noir",
                style = "Streetwear Minimalist",
                season = "All-Season",
                formality = "Casual",
                formalityScore = 4,
                fabric = "280 GSM Combed Cotton",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemStreetTee,
                imageUri = null,
                wearCount = 8,
                lastWornDate = "3 days ago",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Pleated Cargo Trousers",
                category = "Bottoms",
                color = "Olive Drab",
                style = "Athletic Luxury",
                season = "All-Season",
                formality = "Elevated Casual",
                formalityScore = 5,
                fabric = "Technical Washed Twill",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemCargoPants,
                imageUri = null,
                wearCount = 12,
                lastWornDate = "Yesterday",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Technical Bomber Jacket",
                category = "Outerwear",
                color = "Matte Charcoal",
                style = "Athletic Luxury",
                season = "Fall / Winter",
                formality = "Smart Casual",
                formalityScore = 7,
                fabric = "Storm-Resistant Stretch Nylon",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemTechBomber,
                imageUri = null,
                wearCount = 6,
                lastWornDate = "Last week",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Minimal Leather Low-Tops",
                category = "Shoes",
                color = "Triple White",
                style = "Clean Luxury",
                season = "All-Season",
                formality = "Smart Casual",
                formalityScore = 6,
                fabric = "Italian Nappa Leather",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemCourtSneakers,
                imageUri = null,
                wearCount = 22,
                lastWornDate = "Today",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Waffle-Knit Mockneck",
                category = "Knitwear",
                color = "Sand Beige",
                style = "Cozy Minimalist",
                season = "Fall / Winter",
                formality = "Smart Casual",
                formalityScore = 6,
                fabric = "100% Organic Waffle Cotton",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemWaffleKnit,
                imageUri = null,
                wearCount = 14,
                lastWornDate = "4 days ago",
                createdAt = System.currentTimeMillis()
            ),
            ClothingItemEntity(
                name = "Tactical Crossbody Sling",
                category = "Accessories",
                color = "Matte Noir",
                style = "Technical Utility",
                season = "All-Season",
                formality = "Casual",
                formalityScore = 5,
                fabric = "Technical Cordura & Gunmetal",
                isFavorite = true,
                imageRes = WearWiseDrawables.ItemSlingBag,
                imageUri = null,
                wearCount = 19,
                lastWornDate = "Yesterday",
                createdAt = System.currentTimeMillis()
            )
        )
        streetwearItems.forEach { database.clothingItemDao().insertClothingItem(it) }
    }
}
