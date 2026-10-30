package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.sync.SyncManager
import com.example.domain.model.ClothingItem
import com.example.domain.repository.WardrobeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WardrobeRepositoryImpl(
    private val database: AppDatabase,
    private val syncManager: SyncManager
) : WardrobeRepository {

    override fun getAllClothingItems(): Flow<List<ClothingItem>> {
        return database.clothingItemDao().getAllClothingItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getClothingItemsByCategory(category: String): Flow<List<ClothingItem>> {
        return if (category == "All Items" || category.isEmpty()) {
            getAllClothingItems()
        } else {
            database.clothingItemDao().getClothingItemsByCategory(category).map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    override suspend fun addClothingItem(item: ClothingItem): Long {
        val id = database.clothingItemDao().insertClothingItem(item.toEntity())
        syncManager.enqueue("CREATE", "wardrobe", id)
        return id
    }

    override suspend fun updateClothingItem(item: ClothingItem) {
        database.clothingItemDao().updateClothingItem(item.toEntity())
        syncManager.enqueue("UPDATE", "wardrobe", item.id)
    }

    override suspend fun deleteClothingItem(id: Long) {
        database.clothingItemDao().deleteClothingItemById(id)
        syncManager.enqueue("DELETE", "wardrobe", id)
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        database.clothingItemDao().toggleFavorite(id, isFavorite)
        syncManager.enqueue("UPDATE", "wardrobe", id)
    }
}
