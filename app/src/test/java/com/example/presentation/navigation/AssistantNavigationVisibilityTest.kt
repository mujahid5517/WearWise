package com.example.presentation.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantNavigationVisibilityTest {

    @Test
    fun `assistant button is shown on app screens including Shop`() {
        assertTrue(shouldShowAssistantButton(Screen.Outfits.route))
        assertTrue(shouldShowAssistantButton(Screen.Profile.route))
        assertTrue(shouldShowAssistantButton(Screen.Trips.route))
        assertTrue(shouldShowAssistantButton(Screen.Shopping.route))
    }

    @Test
    fun `assistant button is hidden only on authentication and assistant chat screens`() {
        assertFalse(shouldShowAssistantButton(Screen.Welcome.route))
        assertFalse(shouldShowAssistantButton(Screen.CreateAccount.route))
        assertFalse(shouldShowAssistantButton(Screen.SignIn.route))
        assertFalse(shouldShowAssistantButton(Screen.Onboarding.route))
        assertFalse(shouldShowAssistantButton(Screen.AssistantChat.route))
    }
}
