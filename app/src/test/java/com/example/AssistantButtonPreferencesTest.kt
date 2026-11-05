package com.example

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.data.local.AssistantButtonPosition
import com.example.data.local.AssistantButtonPreferences
import com.example.data.local.calculateSnappedAssistantButtonPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AssistantButtonPreferencesTest {

    @Test
    fun `position is persisted between preference instances`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = AssistantButtonPreferences(context)
        val position = AssistantButtonPosition(isOnLeft = true, verticalFraction = 0.72f)

        preferences.savePosition(position)

        assertEquals(position, AssistantButtonPreferences(context).getPosition())
    }

    @Test
    fun `saved vertical placement is clamped to screen bounds`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = AssistantButtonPreferences(context)

        preferences.savePosition(AssistantButtonPosition(isOnLeft = false, verticalFraction = 1.5f))

        assertEquals(1f, preferences.getPosition().verticalFraction)
    }

    @Test
    fun `invalid vertical placement falls back to center`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = AssistantButtonPreferences(context)

        preferences.savePosition(AssistantButtonPosition(isOnLeft = false, verticalFraction = Float.NaN))

        assertEquals(0.5f, preferences.getPosition().verticalFraction)
    }

    @Test
    fun `drag placement snaps to nearest edge and clamps vertical position`() {
        val position = calculateSnappedAssistantButtonPosition(
            x = 20f,
            y = 900f,
            screenWidth = 400f,
            screenHeight = 800f,
            buttonSize = 56f,
            margin = 12f
        )

        assertTrue(position.isOnLeft)
        assertEquals(1f, position.verticalFraction)
    }
}
