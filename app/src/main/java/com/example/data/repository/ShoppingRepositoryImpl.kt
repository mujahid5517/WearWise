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
        val itemCounts = wardrobe.map { it.category }.groupingBy { it }.eachCount()
        val topCategories = itemCounts.entries.sortedByDescending { it.value }.take(3).map { it.key }
        val duplicateNames = wardrobe.map { it.name }.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.toList()
        val summary = if (wardrobe.isEmpty()) {
            "Your wardrobe is empty. Add clothing items to get personalized shopping recommendations."
        } else {
            "Based on your current wardrobe (${wardrobe.size} items), adding versatile foundational pieces will increase outfit combinations. You have items in: ${topCategories.joinToString(", ")}."
        }
        val recommendations = generateRecommendations(wardrobe, profile, budget, currency, occasion, weather, focusCategory)
        return ShoppingAnalysis(
            summary = summary,
            duplicateNotes = duplicateNames.distinct().take(5).map { "You already have $it" },
            recommendations = recommendations
        )
    }

    private suspend fun generateRecommendations(
        wardrobe: List<ClothingItem>,
        profile: UserProfile?,
        budget: Double,
        currency: String,
        occasion: String,
        weather: String?,
        focusCategory: String?
    ): List<ShoppingRecommendation> {
        val recommendations = mutableListOf<ShoppingRecommendation>()

        val tops = wardrobe.filter { it.category.contains("Top", ignoreCase = true) }
        val bottoms = wardrobe.filter { it.category.contains("Bottom", ignoreCase = true) }
        val shoes = wardrobe.filter { it.category.contains("Shoe", ignoreCase = true) }

        if (tops.isNotEmpty() && bottoms.isNotEmpty()) {
            recommendations.add(
                ShoppingRecommendation(
                    name = "Classic White Shirt",
                    category = "Knitwear",
                    priority = "HIGH",
                    why = "Pairs with your existing bottoms for casual looks",
                    suggestedColor = "White",
                    suggestedStyle = "Smart Casual",
                    budgetTarget = budget.coerceAtLeast(50.0),
                    compatibleItems = listOf(tops.first().name),
                    outfitCombinationCount = tops.size * bottoms.size
                )
            )
        }

        if (shoes.isNotEmpty()) {
            recommendations.add(
                ShoppingRecommendation(
                    name = "Versatile Black Sneakers",
                    category = "Footwear",
                    priority = "MEDIUM",
                    why = "Works with most outfits in your wardrobe",
                    suggestedColor = "Black",
                    suggestedStyle = "Casual",
                    budgetTarget = budget.coerceAtLeast(80.0),
                    compatibleItems = listOf(shoes.first().name),
                    outfitCombinationCount = shoes.size
                )
            )
        }

        if (recommendations.isEmpty()) {
            recommendations.add(
                ShoppingRecommendation(
                    name = "Essential Piece",
                    category = "Accessories",
                    priority = "MEDIUM",
                    why = "Add accessories to refresh existing outfits",
                    suggestedColor = "Navy",
                    suggestedStyle = "Classic",
                    budgetTarget = budget.coerceAtLeast(30.0),
                    compatibleItems = wardrobe.map { it.name }.take(3),
                    outfitCombinationCount = 3
                )
            )
        }

        return recommendations
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
        val itemNames = wardrobe.map { it.name }.filter { it.isNotBlank() }.take(5).joinToString(", ")
        val categorySummary = wardrobe.map { it.category }.distinct().take(3).joinToString(", ")
        val safeWeather = weather.orEmpty()

        return when {
            question.isNotBlank() && question.lowercase().contains("outfit") ->
                "Based on your $categorySummary wardrobe, you can create ${wardrobe.size} different outfit combinations. Consider adding $itemNames to expand your options."
            question.isNotBlank() && question.lowercase().contains("budget") ->
                "Your budget is $currency $budget. Focus on versatile pieces that work across multiple occasions."
            question.isNotBlank() && question.lowercase().contains("weather") && safeWeather.isNotBlank() ->
                "For $safeWeather weather, prioritize ${if (safeWeather.lowercase().contains("rain")) "water-resistant" else "warm"} clothing items."
            else -> "I recommend looking for $itemNames that complement your $categorySummary collection."
        }
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
