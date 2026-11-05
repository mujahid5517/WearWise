package com.example.data.sync

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ConnectivityMonitor(context: Context) {
    val isOnline: StateFlow<Boolean> = MutableStateFlow(true)
}
