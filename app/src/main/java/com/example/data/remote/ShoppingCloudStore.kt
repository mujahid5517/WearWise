package com.example.data.remote

import com.example.domain.model.ShoppingBudget
import com.example.domain.model.ShoppingItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ShoppingCloudStore(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val signedInUid = MutableStateFlow(auth.currentUser?.uid.orEmpty())

    fun observeAuthState(listener: () -> Unit) {
        auth.addAuthStateListener {
            signedInUid.value = it.currentUser?.uid.orEmpty()
            listener()
        }
    }

    suspend fun getItems(): List<ShoppingItem> {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) return emptyList()
        val rows = firestore.collection("shopping_items").document(uid).get().await()
            .get("items") as? List<Map<String, Any?>> ?: return emptyList()
        return rows.mapNotNull { row ->
            val remoteId = row["remoteId"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ShoppingItem(
                remoteId = remoteId,
                ownerUid = uid,
                name = row["name"]?.toString().orEmpty(),
                category = row["category"]?.toString().orEmpty(),
                estimatedPrice = (row["estimatedPrice"] as? Number)?.toDouble() ?: 0.0,
                currency = row["currency"]?.toString() ?: "USD",
                shoppingUrl = row["shoppingUrl"]?.toString().orEmpty(),
                reason = row["reason"]?.toString().orEmpty(),
                priority = row["priority"]?.toString() ?: "MEDIUM",
                status = row["status"]?.toString() ?: "NEEDED",
                favorite = row["favorite"] as? Boolean ?: false,
                quantity = ((row["quantity"] as? Number)?.toInt() ?: 1).coerceAtLeast(1),
                actualPrice = (row["actualPrice"] as? Number)?.toDouble(),
                purchasedAt = (row["purchasedAt"] as? Number)?.toLong(),
                isDeleted = row["isDeleted"] as? Boolean ?: false,
                createdAt = (row["createdAt"] as? Number)?.toLong() ?: 0L,
                updatedAt = (row["updatedAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    suspend fun saveItem(item: ShoppingItem) {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank() || item.ownerUid != uid) return
        val document = firestore.collection("shopping_items").document(uid)
        val data = document.get().await().data.orEmpty()
        val items = (data["items"] as? List<Map<String, Any?>>).orEmpty().toMutableList()
        val remoteId = item.remoteId ?: UUID.randomUUID().toString()
        val newRow = mapOf(
            "remoteId" to remoteId,
            "name" to item.name,
            "category" to item.category,
            "estimatedPrice" to item.estimatedPrice,
            "currency" to item.currency,
            "shoppingUrl" to item.shoppingUrl,
            "reason" to item.reason,
            "priority" to item.priority,
            "status" to item.status,
            "favorite" to item.favorite,
            "quantity" to item.quantity.coerceAtLeast(1),
            "actualPrice" to item.actualPrice,
            "purchasedAt" to item.purchasedAt,
            "isDeleted" to item.isDeleted,
            "createdAt" to item.createdAt,
            "updatedAt" to item.updatedAt
        )
        items.removeAll { it["remoteId"]?.toString() == remoteId }
        items.add(newRow)
        document.set(data + ("items" to items)).await()
    }

    suspend fun getBudget(): ShoppingBudget? {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) return null
        val data = firestore.collection("shopping_items").document(uid).get().await().data ?: return null
        val amount = (data["budgetAmount"] as? Number)?.toDouble() ?: return null
        return ShoppingBudget(
            amount = amount,
            currency = data["budgetCurrency"]?.toString() ?: "USD",
            updatedAt = (data["budgetUpdatedAt"] as? Number)?.toLong() ?: 0L
        )
    }

    suspend fun saveBudget(budget: ShoppingBudget) {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) return
        val document = firestore.collection("shopping_items").document(uid)
        val data = document.get().await().data.orEmpty()
        document.set(
            data + mapOf(
                "budgetAmount" to budget.amount.coerceAtLeast(0.0),
                "budgetCurrency" to budget.currency,
                "budgetUpdatedAt" to budget.updatedAt
            )
        ).await()
    }
}
