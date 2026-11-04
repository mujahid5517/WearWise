package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.sync.SyncManager
import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.domain.model.WeeklyScheduleItem
import com.example.domain.model.WearHistory
import com.example.domain.model.WeatherForecast
import com.example.domain.model.WeatherDescriptions
import com.example.data.local.entity.WearHistoryEntity
import com.example.domain.repository.OutfitRepository
import com.google.firebase.auth.FirebaseAuth
import com.example.presentation.common.WearWiseDrawables
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await

class OutfitRepositoryImpl(
    private val database: AppDatabase,
    private val syncManager: SyncManager,
    private val auth: FirebaseAuth? = null,
    private val functions: FirebaseFunctions? = null
) : OutfitRepository {

    override fun getAllOutfits(): Flow<List<Outfit>> {
        return database.outfitDao().getAllOutfits().map { entities ->
            entities.filter { it.ownerUid == auth?.currentUser?.uid.orEmpty() }.map { entity ->
                val clothingIds = database.outfitDao().getClothingIdsForOutfit(entity.id)
                val items = clothingIds.mapNotNull { id ->
                    database.clothingItemDao().getClothingItemById(id)?.toDomain()
                }.filter { it.ownerUid == auth?.currentUser?.uid.orEmpty() }
                entity.toDomain(items)
            }
        }
    }

    override fun getFavoriteOutfits(): Flow<List<Outfit>> {
        return database.outfitDao().getFavoriteOutfits().map { entities ->
            entities.filter { it.ownerUid == auth?.currentUser?.uid.orEmpty() }.map { entity ->
                val clothingIds = database.outfitDao().getClothingIdsForOutfit(entity.id)
                val items = clothingIds.mapNotNull { id ->
                    database.clothingItemDao().getClothingItemById(id)?.toDomain()
                }.filter { it.ownerUid == auth?.currentUser?.uid.orEmpty() }
                entity.toDomain(items)
            }
        }
    }

    override suspend fun getDailyRecommendation(): Outfit {
        val ownerUid = auth?.currentUser?.uid.orEmpty()
        val wardrobe = database.clothingItemDao().getAllClothingItemsSnapshot()
            .map { it.toDomain() }
            .filter { it.ownerUid == ownerUid }
        if (wardrobe.isNotEmpty()) {
            val feedback = database.outfitFeedbackDao().observe(ownerUid).first()
            return buildWardrobeRecommendation(wardrobe, feedback).copy(ownerUid = ownerUid)
        }
        val outfits = database.outfitDao().getAllOutfits().first()
            .filter { it.ownerUid == ownerUid }
        return if (ownerUid.isNotBlank()) {
            Outfit(
                id = 0L,
                title = "Start with your wardrobe",
                occasion = "Everyday",
                style = "Personal style",
                score = 0,
                rationale = "Add a few clothing pieces to get outfit ideas made only from items you own."
            )
        } else if (outfits.isNotEmpty()) {
            val entity = outfits.first()
            val clothingIds = database.outfitDao().getClothingIdsForOutfit(entity.id)
            val items = clothingIds.mapNotNull { id ->
                database.clothingItemDao().getClothingItemById(id)?.toDomain()
            }
            entity.toDomain(items)
        } else {
            // Default Academic Tailor recommendation
            Outfit(
                id = 1L,
                title = "The Academic Tailor",
                occasion = "Smart Academic",
                style = "Smart Casual",
                score = 98,
                rationale = "Tailored specifically for your lecturer schedule. The 100% Oxford cotton weave ensures all-day breathability during the Faculty Meeting, while the structured navy chinos project relaxed academic authority at the 2:00 PM Colloquium.",
                weatherTag = "Check live weather in Planner",
                previewImageRes = WearWiseDrawables.AcademicLook
            )
        }
    }

    override suspend fun generateAiRecommendation(): Outfit? =
        generateAiOutfit("", "Everyday") ?: localWardrobeRecommendation("Everyday", "Offline wardrobe match")

    override suspend fun generateWeatherRecommendation(weather: WeatherForecast, occasion: String): Outfit? {
        val today = "Current conditions: ${weather.temperatureC ?: "unknown"}°C, feels like ${weather.feelsLikeC ?: "unknown"}°C, ${WeatherDescriptions.describe(weather.weatherCode)}, rain chance ${weather.precipitationProbability ?: "unknown"}%, humidity ${weather.humidityPercent ?: "unknown"}%, wind ${weather.windKmh ?: "unknown"} km/h."
        val forecast = weather.daily.take(4).joinToString("; ") { "${it.date}: ${WeatherDescriptions.describe(it.weatherCode ?: 0)}, high ${it.highC}°C low ${it.lowC}°C, rain ${it.precipitationProbability}%" }
        val weatherContext = "$today Forecast: $forecast"
        return generateAiOutfit(weatherContext, occasion)
    }

    private suspend fun generateAiOutfit(weatherContext: String, occasion: String): Outfit? {
        val uid = auth?.currentUser?.uid.orEmpty()
        val wardrobe = database.clothingItemDao().getAllClothingItemsSnapshot()
            .map { it.toDomain() }
            .filter { it.ownerUid == uid }
        if (wardrobe.isEmpty()) return null
        val feedbackRows = database.outfitFeedbackDao().observe(uid).first()
        val positivePreferences = if (feedbackRows.size >= 3) feedbackRows
            .filter { (it.rating ?: 3) >= 4 || it.reaction == "LIKE" }
            .flatMap { listOf(it.colors, it.categories, it.style).flatMap { value -> value.split(",") } }
            .map(String::trim).filter(String::isNotBlank).groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(8).joinToString(", ") { it.key } else ""
        val avoidPreferences = if (feedbackRows.size >= 3) feedbackRows
            .filter { (it.rating ?: 3) <= 2 || it.reaction == "DISLIKE" }
            .flatMap { listOf(it.colors, it.categories, it.style).flatMap { value -> value.split(",") } }
            .map(String::trim).filter(String::isNotBlank).groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(5).joinToString(", ") { it.key } else ""
        val callable = functions ?: return null
        val cloudIds = wardrobe.mapNotNull { item -> item.remoteId?.takeIf(String::isNotBlank) }
            .distinct().take(30)
        if (cloudIds.size < 2) return null
        val positive = feedbackRows.filter { (it.rating ?: 3) >= 4 || it.reaction == "LIKE" }
        val negative = feedbackRows.filter { (it.rating ?: 3) <= 2 || it.reaction == "DISLIKE" }
        val userProfile = database.userProfileDao().getUserProfile().first()
        val request = mapOf(
            "itemIds" to cloudIds,
            "occasion" to occasion,
            "weather" to weatherContext,
            "preferences" to mapOf(
                "styles" to (userProfile?.stylePreferences.orEmpty().split(",").filter(String::isNotBlank) + positive.map { it.style }).distinct().take(10),
                "colors" to (userProfile?.favoriteColors.orEmpty().split(",").filter(String::isNotBlank) + positive.flatMap { it.colors.split(",") }).distinct().take(10),
                "avoidColors" to (userProfile?.avoidedColors.orEmpty().split(",").filter(String::isNotBlank) + negative.flatMap { it.colors.split(",") }).distinct().take(10),
                "liked" to positive.map { "${it.outfitTitle}: ${it.categories} ${it.colors} ${it.style}" }.take(15),
                "disliked" to negative.map { "${it.outfitTitle}: ${it.categories} ${it.colors} ${it.style}" }.take(15)
            )
        )
        return try {
            withTimeout(110_000) {
                val result = callable.getHttpsCallable("generateOutfit").call(request).await().data as? Map<*, *> ?: return@withTimeout null
                val requestedIds = result["selectedItemIds"] as? List<*> ?: return@withTimeout null
                val byRemoteId = wardrobe.filter { it.remoteId != null }.associateBy { it.remoteId }
                val selected = requestedIds.mapNotNull { byRemoteId[it as? String] }
                if (selected.size < 2 || selected.size != requestedIds.size) return@withTimeout null
                val colorExplanation = result["colorCompatibilityExplanation"] as? String ?: ""
                val occasionExplanation = result["occasionSuitability"] as? String ?: ""
                val explanation = result["recommendationExplanation"] as? String ?: "A compatible combination selected from your wardrobe."
                val compatibility = (result["compatibilityScore"] as? Number)?.toInt()?.coerceIn(0, 100) ?: 75
                Outfit(
                    title = result["title"] as? String ?: "Wardrobe match",
                    occasion = occasion,
                    style = result["styleCategory"] as? String ?: "Personal style",
                    score = compatibility,
                    rationale = explanation,
                    weatherTag = weatherContext.ifBlank { "AI wardrobe match" },
                    items = selected,
                    colorHarmonyScore = compatibility,
                    ownerUid = uid,
                    colorCompatibilityExplanation = colorExplanation,
                    occasionSuitability = occasionExplanation,
                    isAiGenerated = true
                )
            }
        } catch (error: Exception) {
            // Keep locally generated, feedback-ranked outfit recommendations available if cloud AI fails.
            null
        }
    }

    private suspend fun localWardrobeRecommendation(occasion: String, weatherContext: String): Outfit? {
        val ownerUid = auth?.currentUser?.uid.orEmpty()
        val wardrobe = database.clothingItemDao().getAllClothingItemsSnapshot()
            .map { it.toDomain() }.filter { it.ownerUid == ownerUid }
        if (wardrobe.isEmpty()) return null
        val feedback = database.outfitFeedbackDao().observe(ownerUid).first()
        val local = buildWardrobeRecommendation(wardrobe, feedback)
        return local.copy(
            occasion = occasion,
            weatherTag = weatherContext.ifBlank { "Offline wardrobe match" },
            rationale = "Offline wardrobe match. ${local.rationale}"
        )
    }

    override suspend fun saveOutfit(outfit: Outfit): Long {
        val ownerUid = auth?.currentUser?.uid.orEmpty()
        require(outfit.items.all { it.ownerUid == ownerUid }) { "Outfits can only use clothing from your signed-in wardrobe." }
        val id = database.outfitDao().insertOutfit(outfit.copy(ownerUid = ownerUid).toEntity())
        val itemLinks = outfit.items.filter { it.id > 0L && it.ownerUid == ownerUid }.map { com.example.data.local.entity.OutfitItemEntity(id, it.id) }
        if (itemLinks.isNotEmpty()) database.outfitDao().insertOutfitItems(itemLinks)
        syncManager.enqueue("CREATE", "outfit", id)
        return id
    }

    private fun buildWardrobeRecommendation(items: List<ClothingItem>, feedback: List<com.example.data.local.entity.OutfitFeedbackEntity>): Outfit {
        fun preference(value: String, selector: (com.example.data.local.entity.OutfitFeedbackEntity) -> String): Double =
            feedback.sumOf { row ->
                val age = ((System.currentTimeMillis() - row.updatedAt).coerceAtLeast(0) / 86_400_000.0)
                val recency = kotlin.math.exp(-age / 90.0)
                val rating = row.rating?.let { (it - 3) / 2.0 } ?: 0.0
                val reaction = when (row.reaction) { "LIKE" -> .75; "DISLIKE" -> -.75; else -> 0.0 }
                val signal = (rating + reaction).coerceIn(-1.0, 1.0) * recency
                if (selector(row).split(",").any { it.equals(value, true) }) signal else 0.0
            }
        fun best(predicate: (ClothingItem) -> Boolean) = items.filter(predicate)
            .sortedByDescending { item ->
                val color = preference(item.color) { it.colors }
                val category = preference(item.category) { it.categories }
                val style = preference(item.style) { it.style }
                val (rating, reaction) = color to category
                (rating + reaction + style).coerceIn(-2.0, 2.0) * 4.0 +
                    (if (item.isFavorite) 2.0 else 0.0) - item.wearCount * .2 +
                    (item.createdAt % 17) / 100.0
            }
            .firstOrNull()

        val dress = best { it.category.contains("dress", true) || it.category.contains("one-piece", true) }
        val selected = if (dress != null) {
            listOfNotNull(dress, best { it.category.equals("Shoes", true) }, best { it.category.equals("Accessories", true) })
        } else {
            listOfNotNull(
                best { it.category.equals("Tops", true) },
                best { it.category.equals("Bottoms", true) },
                best { it.category.equals("Outerwear", true) },
                best { it.category.equals("Shoes", true) },
                best { it.category.equals("Accessories", true) }
            )
        }
        return Outfit(
            id = 0L,
            title = if (selected.isEmpty()) "Build a look from your wardrobe" else "Your wardrobe, styled",
            occasion = selected.firstOrNull()?.occasion?.ifBlank { "Everyday" } ?: "Everyday",
            style = selected.firstOrNull()?.style?.ifBlank { "Personal style" } ?: "Personal style",
            score = if (selected.size >= 3) 96 else 88,
            ownerUid = items.firstOrNull()?.ownerUid.orEmpty(),
            rationale = if (selected.isEmpty()) "Add a top, bottom, or one-piece item to start building outfits from your own clothes." else
                "A practical combination selected from ${selected.size} pieces in your wardrobe, balanced for variety and your feedback preferences.",
            items = selected
        )
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        database.outfitDao().toggleFavorite(id, isFavorite)
        syncManager.enqueue("UPDATE", "outfit", id)
    }

    override suspend fun deleteOutfit(id: Long) {
        database.outfitDao().deleteOutfitById(id)
        database.outfitDao().deleteOutfitItemsByOutfitId(id)
        syncManager.enqueue("DELETE", "outfit", id)
    }

    override fun getWeeklySchedule(): Flow<List<WeeklyScheduleItem>> {
        return database.weeklyScheduleDao().getWeeklySchedule().map { list ->
            list.filter { it.ownerUid == auth?.currentUser?.uid.orEmpty() }.map { it.toDomain() }
        }
    }

    override suspend fun saveScheduleItem(item: WeeklyScheduleItem): Long {
        val ownerUid = auth?.currentUser?.uid.orEmpty()
        if (item.outfitId != null) {
            val outfit = database.outfitDao().getOutfitById(item.outfitId)
            require(outfit?.ownerUid == ownerUid) { "Select an outfit from your own wardrobe." }
        }
        val plan = item.copy(ownerUid = ownerUid)
        val existing = plan.dateIso.takeIf { it.isNotBlank() }?.let {
            database.weeklyScheduleDao().getScheduleForDate(it, ownerUid)
        }
        val id = if (existing == null) database.weeklyScheduleDao().insertSchedule(plan.toEntity())
        else {
            database.weeklyScheduleDao().updateSchedule(plan.copy(id = existing.id).toEntity())
            existing.id
        }
        syncManager.enqueue(if (existing == null) "CREATE" else "UPDATE", "planner", id)
        return id
    }

    override suspend fun updateScheduleItem(item: WeeklyScheduleItem) {
        val current = database.weeklyScheduleDao().getScheduleById(item.id) ?: return
        val ownerUid = auth?.currentUser?.uid.orEmpty()
        require(current.ownerUid == ownerUid) { "This plan belongs to another signed-in account." }
        if (item.outfitId != null) require(database.outfitDao().getOutfitById(item.outfitId)?.ownerUid == ownerUid) {
            "Select an outfit from your own wardrobe."
        }
        val updated = item.copy(ownerUid = ownerUid)
        database.weeklyScheduleDao().updateSchedule(updated.toEntity())
        syncManager.enqueue("UPDATE", "planner", updated.id)
    }

    override suspend fun deleteScheduleItem(id: Long) {
        val current = database.weeklyScheduleDao().getScheduleById(id) ?: return
        require(current.ownerUid == auth?.currentUser?.uid.orEmpty()) { "This plan belongs to another signed-in account." }
        database.weeklyScheduleDao().deleteScheduleById(id)
        syncManager.enqueue("DELETE", "planner", id)
    }

    override suspend fun regenerateWeeklySchedule() {
        val current = database.weeklyScheduleDao().getWeeklySchedule().first()
        val updated = current.map { item ->
            if (item.isUnscheduled) {
                item.copy(isUnscheduled = false, formalityBadge = "Optimized (96%)")
            } else {
                item
            }
        }
        database.weeklyScheduleDao().insertAll(updated)
        // Enqueue updates for all schedule items
        updated.forEach { syncManager.enqueue("UPDATE", "planner", it.id) }
    }

    override fun getWearHistory(): Flow<List<WearHistory>> =
        database.wearHistoryDao().observe(auth?.currentUser?.uid.orEmpty()).map { rows -> rows.map { it.toDomain() } }

    override suspend fun recordOutfitWorn(outfitId: Long) {
        val ownerUid = auth?.currentUser?.uid.orEmpty()
        val outfit = database.outfitDao().getOutfitById(outfitId) ?: error("The saved outfit could not be found.")
        require(outfit.ownerUid == ownerUid) { "This outfit belongs to another signed-in account." }
        val ids = database.outfitDao().getClothingIdsForOutfit(outfitId).filter { id ->
            database.clothingItemDao().getClothingItemById(id)?.ownerUid == ownerUid
        }
        val timestamp = System.currentTimeMillis()
        database.wearHistoryDao().insert(
            WearHistoryEntity(
                outfitId = outfitId,
                outfitTitle = outfit.title,
                clothingItemIds = ids.joinToString(","),
                wornAtEpochMillis = timestamp,
                occasion = outfit.occasion,
                ownerUid = ownerUid
            )
        )
        val day = java.time.LocalDate.now().toString()
        ids.forEach { database.clothingItemDao().markWorn(it, day) }
    }
}
