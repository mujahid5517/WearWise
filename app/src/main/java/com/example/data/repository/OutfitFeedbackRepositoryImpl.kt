package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.OutfitFeedbackEntity
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.Outfit
import com.example.domain.repository.OutfitFeedback
import com.example.domain.repository.OutfitFeedbackRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OutfitFeedbackRepositoryImpl(
    private val database: AppDatabase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val connectivityMonitor: Any? = null
) : OutfitFeedbackRepository {

    override fun observeFeedback(): Flow<List<OutfitFeedback>> {
        val uid = auth.currentUser?.uid.orEmpty()
        return database.outfitFeedbackDao().observe(uid).map { rows ->
            rows.map { it.toDomain() }
        }
    }

    override suspend fun getFeedback(outfit: Outfit): OutfitFeedback? = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid.orEmpty()
        database.outfitFeedbackDao().get(uid, outfit.id)?.toDomain()
    }

    override suspend fun saveFeedback(outfit: Outfit, rating: Int?, reaction: String?, comment: String) {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) return
        val now = System.currentTimeMillis()
        val entity = OutfitFeedbackEntity(
            feedbackId = 0L,
            ownerUid = uid,
            outfitId = outfit.id,
            rating = rating,
            reaction = reaction,
            comment = comment,
            outfitTitle = outfit.title,
            occasion = outfit.occasion,
            style = outfit.style,
            colors = outfit.colorCompatibilityExplanation,
            categories = outfit.occasionSuitability,
            itemNames = outfit.items.joinToString(",") { it.name },
            createdAt = now,
            updatedAt = now,
            synced = true,
            deleted = false
        )
        database.outfitFeedbackDao().upsert(entity)
    }

    override suspend fun updateFeedback(feedback: OutfitFeedback) {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) return
        val now = System.currentTimeMillis()
        val entity = feedback.toEntity(uid).copy(updatedAt = now, synced = true)
        database.outfitFeedbackDao().upsert(entity)
    }

    override suspend fun removeFeedback(outfitId: Long) {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) return
        database.outfitFeedbackDao().delete(uid, outfitId)
    }

    override suspend fun preferenceSummary(): Map<String, List<Pair<String, Int>>> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid.orEmpty()
        val rows = database.outfitFeedbackDao().observe(uid).firstOrNull().orEmpty()
        val positives = rows.filter { (it.rating ?: 3) >= 4 || it.reaction == "LIKE" }
        val negatives = rows.filter { (it.rating ?: 3) <= 2 || it.reaction == "DISLIKE" }

        fun topStats(items: List<OutfitFeedbackEntity>, extractor: (OutfitFeedbackEntity) -> String): List<Pair<String, Int>> {
            return items.flatMap { extractor(it).split(",") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { Pair(it.key, it.value) }
        }

        mapOf(
            "likedColors" to topStats(positives) { it.colors },
            "likedCategories" to topStats(positives) { it.categories },
            "likedStyles" to topStats(positives) { it.style },
            "dislikedColors" to topStats(negatives) { it.colors },
            "dislikedCategories" to topStats(negatives) { it.categories },
            "dislikedStyles" to topStats(negatives) { it.style }
        )
    }
}
