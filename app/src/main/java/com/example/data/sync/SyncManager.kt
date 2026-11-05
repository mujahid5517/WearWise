package com.example.data.sync

import com.example.data.local.AppDatabase
import com.example.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
}

class SyncManager(
    private val database: AppDatabase,
    private val connectivityMonitor: ConnectivityMonitor? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val signedInUid = MutableStateFlow("")

    fun start() {
        connectivityMonitor?.isOnline?.onEach { online ->
            if (online) processQueue()
        }?.launchIn(scope)
    }

    fun observeAuthState(listener: () -> Unit = {}) {
        listener()
    }

    suspend fun enqueue(operation: String, entityType: String, entityId: Long) {
        database.syncQueueDao().insert(
            SyncQueueEntity(
                operation = operation,
                entityType = entityType,
                entityId = entityId
            )
        )
        processQueue()
    }

    suspend fun clearQueueOnSignOut() {
        database.syncQueueDao().clearAll()
        _syncState.value = SyncState.Idle
    }

    private fun processQueue() {
        scope.launch {
            _syncState.value = SyncState.Syncing
            runCatching {
                // Background queue processing
            }
            _syncState.value = SyncState.Idle
        }
    }
}
