package com.example.data.sync

import com.example.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SyncManager(
    private val database: AppDatabase,
    private val connectivityMonitor: ConnectivityMonitor
) {
    val syncState: StateFlow<Boolean> = MutableStateFlow(false)
    fun start() {}
    fun enqueue(action: String, entity: String, id: Long) {}
}
