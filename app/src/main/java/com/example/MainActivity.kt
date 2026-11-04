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

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = AppContainer(this)

        // Cloud features are available when google-services.json is configured; local app features
        // remain usable without Firebase configuration.
        val firebaseApp = container.firebaseApp
        if (firebaseApp != null) {
            Log.d("WearWiseFirebase", "Firebase initialized successfully")
            Log.d("WearWiseFirebase", "Project ID: ${firebaseApp.options.projectId}")
        } else {
            Log.w("WearWiseFirebase", "Firebase is not configured; continuing with local features")
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
