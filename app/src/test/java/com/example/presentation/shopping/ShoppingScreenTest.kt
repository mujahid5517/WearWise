package com.example.presentation.shopping

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShoppingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `assistant close button returns through navigation callback`() {
        var closeCount = 0

        composeRule.setContent {
            ShoppingScreen(
                showCloseButton = true,
                onNavigateBack = { closeCount++ }
            )
        }

        composeRule.onNodeWithContentDescription("Close assistant").performClick()

        assertEquals(1, closeCount)
    }

    @Test
    fun `shop screen is empty and does not render assistant chat`() {
        composeRule.setContent {
            EmptyShopScreen()
        }

        composeRule.onNodeWithTag("shop_screen_empty").assertExists()
        assertTrue(composeRule.onAllNodesWithText("AI FASHION ASSISTANT").fetchSemanticsNodes().isEmpty())
        assertTrue(
            composeRule.onAllNodesWithContentDescription("Open AI Fashion Assistant")
                .fetchSemanticsNodes()
                .isEmpty()
        )
    }
}
