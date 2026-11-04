package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.domain.model.Outfit
import com.example.domain.repository.OutfitFeedback
import com.example.domain.repository.OutfitFeedbackRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class OutfitFeedbackRepositoryImpl(
    private val database: AppDatabase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val connectivityMonitor: Any? = null
) : OutfitFeedbackRepository {

    override fun observeFeedback(): Flow<List<OutfitFeedback>> = flowOf(emptyList())

    override suspend fun getFeedback(outfit: Outfit): OutfitFeedback? = null

    override suspend fun saveFeedback(outfit: Outfit, rating: Int?, reaction: String?, comment: String) {}

    override suspend fun updateFeedback(feedback: OutfitFeedback) {}

    override suspend fun removeFeedback(outfitId: Long) {}

    override suspend fun preferenceSummary(): Map<String, List<Pair<String, Int>>> = emptyMap()
}
