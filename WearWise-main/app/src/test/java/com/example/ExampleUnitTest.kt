package com.example

import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `default user profile has Cambridge microclimate`() {
        val user = UserProfile()
        assertEquals("Yared", user.firstName)
        assertEquals("Cambridge, MA", user.region)
        assertTrue(user.favoriteColors.contains("Deep Navy"))
        assertTrue(user.avoidedColors.contains("Neon Yellow"))
    }

    @Test
    fun `curated outfit score calculation`() {
        val outfit = Outfit(
            id = 1L,
            title = "The Academic Tailor",
            occasion = "Smart Academic",
            style = "Smart Casual",
            score = 98,
            rationale = "Tailored for university schedule"
        )
        assertEquals(98, outfit.score)
        assertEquals("Smart Academic", outfit.occasion)
    }

    @Test
    fun `clothing item defaults are sound`() {
        val item = ClothingItem(
            name = "Navy Tailored Chinos",
            category = "Bottoms",
            color = "Deep Navy",
            style = "Minimalist"
        )
        assertEquals("All-Season", item.season)
        assertEquals("Smart Casual", item.formality)
        assertEquals(7, item.formalityScore)
    }
}
