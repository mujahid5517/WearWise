package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.di.AppContainer
import com.example.presentation.navigation.WearWiseApp
import com.example.ui.theme.WearWiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = AppContainer(this)
        setContent {
            WearWiseTheme {
                WearWiseApp(container = container)
            }
        }
    }
}
