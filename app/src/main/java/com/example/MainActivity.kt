package com.example

import android.os.Bundle
import android.util.Log
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.example.di.AppContainer
import com.example.presentation.navigation.WearWiseApp
import com.example.ui.theme.WearWiseTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = AppContainer(this)

        // Firebase Authentication restores its persisted user session automatically.
        try {
            val firebaseApp = FirebaseApp.getInstance()
            val firestore = container.firestore

            Log.d("WearWiseFirebase", "Firebase initialized successfully")
            Log.d("WearWiseFirebase", "Project ID: ${firebaseApp.options.projectId}")
            Log.d("WearWiseFirebase", "Firestore instance: $firestore")
        } catch (e: Exception) {
            Log.e("WearWiseFirebase", "Firebase initialization failed", e)
        }

        setContent {
            val themeMode by container.themePreferences.themeMode.collectAsState()
            val darkTheme = themeMode.resolvesToDark(isSystemInDarkTheme())
            SideEffect {
                window.statusBarColor = AndroidColor.TRANSPARENT
                window.navigationBarColor = AndroidColor.TRANSPARENT
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            WearWiseTheme(darkTheme = darkTheme) {
                WearWiseApp(container = container)
            }
        }
    }
}
