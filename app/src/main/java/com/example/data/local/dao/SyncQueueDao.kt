package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(operation: SyncQueueEntity): Long

    @Query("UPDATE sync_queue SET attemptCount = attemptCount + 1, lastAttemptAt = :attemptedAt WHERE id = :id")
    suspend fun recordAttempt(id: Long, attemptedAt: Long)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun remove(id: Long)
}
