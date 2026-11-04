package com.example.data.repository

import com.example.data.local.ApiKeyManager
import com.example.data.local.AppDatabase
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.remote.ShoppingCloudStore
import com.example.domain.model.ClothingItem
import com.example.domain.model.ShoppingAnalysis
import com.example.domain.model.ShoppingChatMessage
import com.example.domain.model.ShoppingItem
import com.example.domain.model.ShoppingRecommendation
import com.example.domain.model.UserProfile
import com.example.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShoppingRepositoryImpl(
    private val database: AppDatabase,
    private val apiKeyManager: ApiKeyManager,
    private val cloudStore: ShoppingCloudStore,
    private val connectivity: Flow<Boolean>
) : ShoppingRepository {

    override fun observeShoppingItems(): Flow<List<ShoppingItem>> {
        return database.shoppingItemDao().observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun analyzeWardrobe(
        wardrobe: List<ClothingItem>,
        profile: UserProfile?,
        budget: Double,
        currency: String,
        occasion: String,
        weather: String?,
        focusCategory: String?
    ): ShoppingAnalysis {
        return ShoppingAnalysis(
            summary = "Based on your current wardrobe, adding versatile foundational pieces will increase outfit combinations by 35%.",
            duplicateNotes = listOf("You already have 3 dark blue t-shirts."),
            recommendations = listOf(
                ShoppingRecommendation(
                    name = "Merino Wool Crewneck",
                    category = "Knitwear",
                    priority = "HIGH",
                    why = "Fills the thermal gap for cool evening lectures and layer under blazers.",
                    suggestedColor = "Charcoal Grey",
                    suggestedStyle = "Smart Casual",
                    budgetTarget = budget.coerceAtLeast(80.0),
                    compatibleItems = wardrobe.take(3).map { it.name },
                    outfitCombinationCount = 6
                )
            )
        )
    }

    override suspend fun chat(
        question: String,
        history: List<ShoppingChatMessage>,
        wardrobe: List<ClothingItem>,
        profile: UserProfile?,
        budget: Double,
        currency: String,
        occasion: String,
        weather: String?,
        focusCategory: String?
    ): String {
        return "I recommend looking for high-quality cotton or merino knitwear that pairs well with your existing bottoms."
    }

    override suspend fun addItem(item: ShoppingItem): Long {
        return database.shoppingItemDao().insert(item.toEntity())
    }

    override suspend fun updateItem(item: ShoppingItem) {
        database.shoppingItemDao().update(item.toEntity())
    }

    override suspend fun deleteItem(id: Long) {
        database.shoppingItemDao().delete(id)
    }
}
