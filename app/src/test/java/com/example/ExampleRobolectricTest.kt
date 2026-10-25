package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.ClothingItem
import com.example.domain.model.UserProfile
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WearWise", appName)
    }

    @Test
    fun `user profile entity mapper round trip`() {
        val profile = UserProfile(
            id = 1L,
            firstName = "Yared",
            nickname = "Dr. Yared",
            email = "yared@institution.edu",
            profession = "Lecturer & Researcher"
        )
        val entity = profile.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals("Yared", mappedBack.firstName)
        assertEquals("Dr. Yared", mappedBack.nickname)
        assertEquals("yared@institution.edu", mappedBack.email)
        assertEquals("Lecturer & Researcher", mappedBack.profession)
    }

    @Test
    fun `clothing item entity mapper round trip`() {
        val item = ClothingItem(
            id = 42L,
            name = "White Oxford Cotton",
            category = "Tops",
            color = "Crisp White",
            style = "Smart Casual",
            formalityScore = 7,
            fabric = "100% Breathable Cotton"
        )
        val entity = item.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(42L, mappedBack.id)
        assertEquals("White Oxford Cotton", mappedBack.name)
        assertEquals("Tops", mappedBack.category)
        assertEquals(7, mappedBack.formalityScore)
    }
}
