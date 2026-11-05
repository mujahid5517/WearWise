package com.example.presentation.shopping

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AssistantButtonPreferences
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AssistantFloatingButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `button is visible and opens assistant when tapped`() {
        var clickCount = 0
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                AssistantFloatingButton(
                    preferences = AssistantButtonPreferences(context),
                    onClick = { clickCount++ }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Open AI Fashion Assistant")
            .assertExists()
            .performClick()

        assertEquals(1, clickCount)
    }
}
