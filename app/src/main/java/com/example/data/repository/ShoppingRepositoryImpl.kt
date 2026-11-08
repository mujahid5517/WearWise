package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.remote.ShoppingCloudStore
import com.example.domain.model.ClothingItem
import com.example.domain.model.ShoppingAnalysis
import com.example.domain.model.ShoppingBudget
import com.example.domain.model.ShoppingChatMessage
import com.example.domain.model.ShoppingItem
import com.example.domain.model.UserProfile
import com.example.domain.repository.ShoppingRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class ShoppingRepositoryImpl(
    private val database: AppDatabase,
    private val cloudStore: ShoppingCloudStore,
    private val connectivity: Flow<Boolean>
) : ShoppingRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _syncMessage = MutableStateFlow<String?>(null)
    override val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    @Volatile private var isOnline = true

    init {
        scope.launch {
            connectivity.collect { online ->
                val becameOnline = online && !isOnline
                isOnline = online
                if (becameOnline || online) runSyncSafely { syncPending() }
            }
        }
        scope.launch { runSyncSafely { syncFromCloud() } }
        cloudStore.observeAuthState {
            scope.launch { runSyncSafely { syncFromCloud(); syncPending() } }
        }
    }

    override fun observeShoppingItems(): Flow<List<ShoppingItem>> {
        return combine(
            database.shoppingItemDao().observeAll(),
            cloudStore.signedInUid
        ) { rows, uid ->
            rows.filter { ownedByCurrentAccount(it.ownerUid, uid) }.map { it.toDomain() }
        }
    }

    override fun observePurchaseHistory(): Flow<List<ShoppingItem>> {
        return combine(
            database.shoppingItemDao().observePurchaseHistory(),
            cloudStore.signedInUid
        ) { rows, uid ->
            rows.filter { ownedByCurrentAccount(it.ownerUid, uid) }.map { it.toDomain() }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeBudget(): Flow<ShoppingBudget> {
        return cloudStore.signedInUid
            .flatMapLatest { uid -> database.shoppingBudgetDao().observe(ownerKey(uid)) }
            .map { it?.toDomain() ?: ShoppingBudget() }
    }

    override suspend fun updateBudget(budget: ShoppingBudget) {
        val normalized = budget.copy(
            amount = budget.amount.coerceAtLeast(0.0),
            currency = budget.currency.ifBlank { "USD" },
            updatedAt = System.currentTimeMillis()
        )
        val owner = ownerKey(cloudStore.signedInUid.value)
        database.shoppingBudgetDao().upsert(normalized.toEntity(owner))
        runSyncSafely { syncBudget() }
    }

    override suspend fun analyzeWardrobe(
        wardrobe: List<ClothingItem>,
        profile: UserProfile?,
        budget: Double,
        currency: String,
        occasion: String,
        weather: String?,
        focusCategory: String?,
        feedback: Map<String, List<Pair<String, Int>>>
    ): ShoppingAnalysis = ShoppingRecommendationEngine.analyze(
        wardrobe, profile, budget, currency, occasion, weather, focusCategory, feedback
    )

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
        val names = wardrobe.map { it.name }.filter { it.isNotBlank() }.take(5)
        val categories = wardrobe.map { it.category }.filter { it.isNotBlank() }.distinct()
        return when {
            wardrobe.isEmpty() ->
                "I can make local recommendations from your closet, but it is empty right now. Add wardrobe items to get personalized gap suggestions."
            question.contains("budget", true) ->
                if (budget > 0) "Your configured shopping budget is $currency $budget. This is separate from recorded purchases."
                else "You have not configured a shopping budget yet. Purchases are tracked separately from the budget."
            question.contains("weather", true) && !weather.isNullOrBlank() ->
                "Your available weather context is $weather. Your wardrobe categories include ${categories.joinToString(", ")}."
            question.contains("feedback", true) ->
                "Recommendations can use your saved outfit feedback. Your current closet categories are ${categories.joinToString(", ")}."
            question.contains("outfit", true) ->
                "From the ${wardrobe.size} real items in your closet (${names.joinToString(", ")}), use your selected $occasion occasion as a styling filter. I do not have a live product catalog."
            else ->
                "Your closet includes ${categories.joinToString(", ")}. I can suggest category gaps based on your saved wardrobe and profile; I do not provide fabricated products or purchase links."
        }
    }

    override suspend fun addItem(item: ShoppingItem): Long {
        val now = System.currentTimeMillis()
        val prepared = item.copy(
            ownerUid = ownerKey(cloudStore.signedInUid.value),
            remoteId = item.remoteId ?: UUID.randomUUID().toString(),
            quantity = item.quantity.coerceAtLeast(1),
            createdAt = now,
            updatedAt = now,
            isDeleted = false
        )
        val id = database.shoppingItemDao().insert(prepared.toEntity())
        syncItem(prepared.copy(id = id))
        return id
    }

    override suspend fun updateItem(item: ShoppingItem) {
        val existing = database.shoppingItemDao().getById(item.id) ?: return
        if (!ownedByCurrentAccount(existing.ownerUid, cloudStore.signedInUid.value)) return
        val updated = item.copy(
            ownerUid = existing.ownerUid,
            remoteId = existing.remoteId ?: item.remoteId ?: UUID.randomUUID().toString(),
            quantity = item.quantity.coerceAtLeast(1),
            updatedAt = System.currentTimeMillis()
        )
        database.shoppingItemDao().update(updated.toEntity())
        syncItem(updated)
    }

    override suspend fun deleteItem(id: Long) {
        val existing = database.shoppingItemDao().getById(id) ?: return
        if (!ownedByCurrentAccount(existing.ownerUid, cloudStore.signedInUid.value)) return
        val deleted = existing.toDomain().copy(
            isDeleted = true,
            updatedAt = System.currentTimeMillis()
        )
        database.shoppingItemDao().update(deleted.toEntity())
        syncItem(deleted)
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        val existing = database.shoppingItemDao().getById(id) ?: return
        if (!ownedByCurrentAccount(existing.ownerUid, cloudStore.signedInUid.value)) return
        database.shoppingItemDao().setFavorite(id, favorite, System.currentTimeMillis())
        database.shoppingItemDao().getById(id)?.toDomain()?.let { syncItem(it) }
    }

    private fun ownerKey(uid: String) = uid.ifBlank { "local" }

    private fun ownedByCurrentAccount(ownerUid: String, activeUid: String): Boolean =
        if (activeUid.isBlank()) ownerUid.isBlank() || ownerUid == "local" else ownerUid == activeUid

    private suspend fun syncFromCloud() {
        val uid = cloudStore.signedInUid.value
        if (!isOnline || uid.isBlank()) return
        runSyncSafely {
            var itemsSynced = true
            cloudStore.getItems().forEach { remote ->
                val dao = database.shoppingItemDao()
                val local = dao.getByRemoteId(remote.remoteId.orEmpty())
                if (local == null) {
                    dao.insert(remote.toEntity())
                } else if (remote.updatedAt >= local.updatedAt) {
                    dao.update(remote.copy(id = local.id).toEntity())
                } else if (!syncItem(local.toDomain())) itemsSynced = false
            }
            syncBudget()
            if (itemsSynced) _syncMessage.value = null
        }
    }

    private suspend fun syncPending() {
        val uid = cloudStore.signedInUid.value
        if (!isOnline || uid.isBlank()) return
        runSyncSafely {
            var itemsSynced = true
            database.shoppingItemDao().snapshot()
                .filter { it.ownerUid == uid }
                .forEach { if (!syncItem(it.toDomain())) itemsSynced = false }
            syncBudget()
            if (itemsSynced) _syncMessage.value = null
        }
    }

    private suspend fun syncItem(item: ShoppingItem): Boolean {
        val uid = cloudStore.signedInUid.value
        if (uid.isBlank()) {
            if (item.ownerUid.isBlank() || item.ownerUid == "local") {
                _syncMessage.value = "Saved on this device. Sign in to sync shopping across devices."
            }
            return false
        }
        if (!isOnline || item.ownerUid != uid) return false
        return try {
            cloudStore.saveItem(item)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            reportSyncFailure(error)
            false
        }
    }

    private suspend fun syncBudget() {
        val uid = cloudStore.signedInUid.value
        if (uid.isBlank()) {
            _syncMessage.value = "Saved on this device. Sign in to sync shopping across devices."
            return
        }
        if (!isOnline) return
        val dao = database.shoppingBudgetDao()
        val local = dao.get(uid)?.toDomain()
        val remote = cloudStore.getBudget()
        when {
            remote != null && (local == null || remote.updatedAt > local.updatedAt) ->
                dao.upsert(remote.toEntity(uid))
            local != null && (remote == null || local.updatedAt >= remote.updatedAt) ->
                cloudStore.saveBudget(local)
        }
    }

    private suspend fun runSyncSafely(block: suspend () -> Unit) {
        runShoppingSyncSafely(::reportSyncFailure, block)
    }

    private fun reportSyncFailure(error: Throwable) {
        _syncMessage.value = shoppingSyncErrorMessage(error)
    }
}

internal suspend fun runShoppingSyncSafely(
    onError: (Throwable) -> Unit,
    block: suspend () -> Unit
) {
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        onError(error)
    }
}

internal fun shoppingSyncErrorMessage(error: Throwable): String {
    val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    return shoppingSyncErrorMessage(firestoreError?.code)
}

internal fun shoppingSyncErrorMessage(code: FirebaseFirestoreException.Code?): String {
    return when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Cloud sync permission denied. Your local shopping data is safe; deploy the shopping_items/{uid} Firestore rule and verify this account owns the document."
        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            "Cloud sync needs an authenticated account. Your local shopping data is safe."
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
            "Cloud sync is temporarily unavailable. Your local shopping data is safe and will retry."
        else ->
            "Shopping cloud sync failed. Your local data is safe; sync will retry when available."
    }
}
