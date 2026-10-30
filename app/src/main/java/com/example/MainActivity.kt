package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.di.AppContainer
import com.example.presentation.navigation.WearWiseApp
import com.example.ui.theme.WearWiseTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = AppContainer(this)

        // Verify Firebase initialization and Anonymous Auth
        try {
            val firebaseApp = FirebaseApp.getInstance()
            val firestore = container.firestore
            val auth = container.auth

            Log.d("WearWiseFirebase", "Firebase initialized successfully")
            Log.d("WearWiseFirebase", "Project ID: ${firebaseApp.options.projectId}")
            Log.d("WearWiseFirebase", "Firestore instance: $firestore")

            // Authenticate test client anonymously
            if (auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        Log.d("WearWiseFirebase", "Anonymous Auth Successful! User ID: ${user?.uid}")
                    }
                    .addOnFailureListener { e ->
                        Log.w("WearWiseFirebase", "Anonymous Auth Failure (ensure Anonymous Auth is enabled in Firebase Console)", e)
                    }
            } else {
                Log.d("WearWiseFirebase", "Already authenticated. Active User ID: ${auth.currentUser?.uid}")
            }

        } catch (e: Exception) {
            Log.e("WearWiseFirebase", "Firebase initialization failed", e)
        }

        setContent {
            WearWiseTheme {
                WearWiseApp(container = container)
            }
        }
    }
}
