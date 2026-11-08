package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val operation: String,
    val entityType: String,
    val entityId: Long,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
