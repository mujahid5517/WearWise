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

        // The Google Services plugin normally supplies these options and initializes the
        // default app through FirebaseInitProvider before Activity.onCreate. Keep an explicit
        // one-time fallback for environments where provider initialization did not run.
        val firebaseApp = FirebaseApp.getApps(this).firstOrNull()
            ?: FirebaseApp.initializeApp(this)
        checkNotNull(firebaseApp) {
            "Firebase is not configured for ${BuildConfig.APPLICATION_ID}. Add a valid " +
                "app/google-services.json for this Android application ID and sync Gradle."
        }
        Log.d("WearWiseFirebase", "Firebase initialized for project ${firebaseApp.options.projectId}")

        // Construct Firebase-dependent repositories only after initialization succeeds.
        val container = AppContainer(this)

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
