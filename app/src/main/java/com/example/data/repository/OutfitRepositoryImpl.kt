package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.sync.SyncManager
import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.domain.model.WeeklyScheduleItem
import com.example.domain.repository.OutfitRepository
import com.example.presentation.common.WearWiseDrawables
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OutfitRepositoryImpl(
    private val database: AppDatabase,
    private val syncManager: SyncManager
) : OutfitRepository {

    override fun getAllOutfits(): Flow<List<Outfit>> {
        return database.outfitDao().getAllOutfits().map { entities ->
            entities.map { entity ->
                val clothingIds = database.outfitDao().getClothingIdsForOutfit(entity.id)
                val items = clothingIds.mapNotNull { id ->
                    database.clothingItemDao().getClothingItemById(id)?.toDomain()
                }
                entity.toDomain(items)
            }
        }
    }

    override fun getFavoriteOutfits(): Flow<List<Outfit>> {
        return database.outfitDao().getFavoriteOutfits().map { entities ->
            entities.map { entity ->
                val clothingIds = database.outfitDao().getClothingIdsForOutfit(entity.id)
                val items = clothingIds.mapNotNull { id ->
                    database.clothingItemDao().getClothingItemById(id)?.toDomain()
                }
                entity.toDomain(items)
            }
        }
    }

    override suspend fun getDailyRecommendation(): Outfit {
        val outfits = database.outfitDao().getAllOutfits().first()
        return if (outfits.isNotEmpty()) {
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
                weatherTag = "Sunny 21°C • University Campus",
                previewImageRes = WearWiseDrawables.AcademicLook
            )
        }
    }

    override suspend fun saveOutfit(outfit: Outfit): Long {
        val id = database.outfitDao().insertOutfit(outfit.toEntity())
        syncManager.enqueue("CREATE", "outfit", id)
        return id
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
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveScheduleItem(item: WeeklyScheduleItem): Long {
        val id = database.weeklyScheduleDao().insertSchedule(item.toEntity())
        syncManager.enqueue("CREATE", "planner", id)
        return id
    }

    override suspend fun updateScheduleItem(item: WeeklyScheduleItem) {
        database.weeklyScheduleDao().updateSchedule(item.toEntity())
        syncManager.enqueue("UPDATE", "planner", item.id)
    }

    override suspend fun deleteScheduleItem(id: Long) {
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
}
