package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.ClothingItem
import com.example.domain.repository.WardrobeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WardrobeRepositoryImpl(
    private val database: AppDatabase
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
        return database.clothingItemDao().insertClothingItem(item.toEntity())
    }

    override suspend fun updateClothingItem(item: ClothingItem) {
        database.clothingItemDao().updateClothingItem(item.toEntity())
    }

    override suspend fun deleteClothingItem(id: Long) {
        database.clothingItemDao().deleteClothingItemById(id)
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        database.clothingItemDao().toggleFavorite(id, isFavorite)
    }
}
