package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ApiKeyManager
import com.example.data.local.dao.TripDao
import com.example.data.local.entity.TripEntity
import com.example.data.repository.TripRepositoryImpl
import com.example.domain.model.ClothingItem
import com.example.domain.model.DailyWeather
import com.example.domain.model.WeatherForecast
import com.example.domain.model.WeatherLocation
import com.example.domain.repository.WeatherRepository
import com.example.domain.model.UserProfile
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WardrobeRepository
import com.example.presentation.trips.CreateTripViewModel
import com.example.presentation.trips.TripDetailViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeTripDao : TripDao {
    private val tripsFlow = MutableStateFlow<List<TripEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<TripEntity>> = tripsFlow

    override suspend fun getById(id: Long): TripEntity? = tripsFlow.value.find { it.id == id }

    override suspend fun insert(trip: TripEntity): Long {
        val id = if (trip.id == 0L) nextId++ else trip.id
        val entity = trip.copy(id = id)
        tripsFlow.value = tripsFlow.value.filterNot { it.id == id } + entity
        return id
    }

    override suspend fun delete(id: Long) {
        tripsFlow.value = tripsFlow.value.filterNot { it.id == id }
    }
}

class FakeWeatherRepository(
    private val rainProbability: Int = 10,
    private val weatherCode: Int = 0,
    private val highC: Double = 22.0,
    private val lowC: Double = 14.0
) : WeatherRepository {
    override suspend fun refresh(location: WeatherLocation, force: Boolean): WeatherForecast {
        val daily = listOf(
            DailyWeather(
                date = "2026-06-01",
                highC = highC,
                lowC = lowC,
                precipitationProbability = rainProbability,
                weatherCode = weatherCode
            ),
            DailyWeather(
                date = "2026-06-02",
                highC = highC,
                lowC = lowC,
                precipitationProbability = rainProbability,
                weatherCode = weatherCode
            )
        )
        return WeatherForecast(
            location = location,
            temperatureC = highC,
            daily = daily
        )
    }

    override suspend fun cachedForecast(): WeatherForecast? = null

    override suspend fun searchLocations(query: String): List<WeatherLocation> {
        return listOf(WeatherLocation(displayName = "$query, Test Country", latitude = 48.8566, longitude = 2.3522, name = query))
    }

    override fun getSavedLocation(): WeatherLocation? = null
    override fun saveLocation(location: WeatherLocation) {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TripPackingAssistantTest {

    private lateinit var fakeDao: FakeTripDao
    private lateinit var apiKeyManager: ApiKeyManager
    private lateinit var tripRepository: TripRepositoryImpl

    private val sampleWardrobe = listOf(
        ClothingItem(
            id = 1L,
            name = "White Oxford Cotton Shirt",
            category = "Tops",
            color = "White",
            style = "Smart Casual"
        ),
        ClothingItem(
            id = 2L,
            name = "Navy Tailored Chinos",
            category = "Bottoms",
            color = "Navy",
            style = "Smart Casual"
        ),
        ClothingItem(
            id = 3L,
            name = "Burnished Leather Loafers",
            category = "Shoes",
            color = "Brown",
            style = "Classic"
        ),
        ClothingItem(
            id = 4L,
            name = "Minimalist Leather Watch",
            category = "Accessories",
            color = "Silver",
            style = "Classic"
        ),
        ClothingItem(
            id = 5L,
            name = "Stained Graphic Tee",
            category = "Tops",
            color = "Grey",
            style = "Casual",
            description = "Currently in laundry bin"
        ),
        ClothingItem(
            id = 6L,
            name = "Ripped Denim Jeans",
            category = "Bottoms",
            color = "Blue",
            style = "Casual",
            occasion = "unavailable - needs repair"
        )
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fakeDao = FakeTripDao()
        apiKeyManager = ApiKeyManager(context)
        tripRepository = TripRepositoryImpl(fakeDao, apiKeyManager)
    }

    @Test
    fun `fallback trip creation prevents recommending unavailable or laundry items`() = runBlocking {
        val tripId = tripRepository.createTrip(
            destination = "Paris",
            departureDate = "2026-06-01",
            returnDate = "2026-06-05",
            weatherPreference = "Sunny & Warm (22°C)",
            occasion = "Vacation / Leisure",
            wardrobe = sampleWardrobe,
            profile = UserProfile(firstName = "Alex")
        )

        val trip = tripRepository.getTripById(tripId)
        assertNotNull(trip)
        assertEquals("Paris", trip?.destination)

        val packedClothingNames = trip!!.packingList.map { it.itemName }
        // Items 5 and 6 are marked laundry / unavailable and must NOT be in packing list
        assertFalse(packedClothingNames.contains("Stained Graphic Tee"))
        assertFalse(packedClothingNames.contains("Ripped Denim Jeans"))

        // Available items 1, 2, 3 should be recommended
        assertTrue(packedClothingNames.contains("White Oxford Cotton Shirt"))
        assertTrue(packedClothingNames.contains("Navy Tailored Chinos"))
    }

    @Test
    fun `packing list organizes recommendations into required categories`() = runBlocking {
        val tripId = tripRepository.createTrip(
            destination = "Tokyo",
            departureDate = "2026-07-01",
            returnDate = "2026-07-04",
            weatherPreference = "Warm (26°C)",
            occasion = "Sightseeing",
            wardrobe = sampleWardrobe,
            profile = null
        )

        val trip = tripRepository.getTripById(tripId)
        assertNotNull(trip)

        val categories = trip!!.packingList.map { it.category }.toSet()
        // Must contain Tops, Bottoms, Shoes, and Essentials
        assertTrue(categories.contains("Tops"))
        assertTrue(categories.contains("Bottoms"))
        assertTrue(categories.contains("Shoes"))
        assertTrue(categories.contains("Essentials"))

        // Standard essentials must be present
        val essentials = trip.packingList.filter { it.category == "Essentials" }
        assertTrue(essentials.any { it.itemName.contains("Passport", ignoreCase = true) })
        assertTrue(essentials.any { it.itemName.contains("Charger", ignoreCase = true) })
        assertTrue(essentials.any { it.itemName.contains("Toiletries", ignoreCase = true) })
    }

    @Test
    fun `generates complete outfit combinations for each travel day`() = runBlocking {
        val departure = "2026-09-10"
        val returnDate = "2026-09-13" // 4 days duration: 10, 11, 12, 13

        val tripId = tripRepository.createTrip(
            destination = "Rome",
            departureDate = departure,
            returnDate = returnDate,
            weatherPreference = "Sunny (25°C)",
            occasion = "Vacation",
            wardrobe = sampleWardrobe,
            profile = null
        )

        val trip = tripRepository.getTripById(tripId)
        assertNotNull(trip)
        assertEquals(4, trip!!.itinerary.size)

        for (i in 1..4) {
            val dayOutfit = trip.itinerary.find { it.dayNumber == i }
            assertNotNull("Day $i outfit must exist", dayOutfit)
            assertTrue("Day $i must have a title", dayOutfit!!.outfitTitle.isNotBlank())
            assertTrue("Day $i must have a rationale", dayOutfit.rationale.isNotBlank())
            assertTrue("Day $i must have clothing items", dayOutfit.clothingItemNames.isNotEmpty())

            // Outfits must NOT contain laundry/unavailable items
            assertFalse(dayOutfit.clothingItemNames.contains("Stained Graphic Tee"))
            assertFalse(dayOutfit.clothingItemNames.contains("Ripped Denim Jeans"))
        }
    }

    @Test
    fun `interactive packing checklist toggles packed status and persists locally`() = runBlocking {
        val tripId = tripRepository.createTrip(
            destination = "London",
            departureDate = "2026-10-01",
            returnDate = "2026-10-03",
            weatherPreference = "Cool & Rainy (14°C)",
            occasion = "Business",
            wardrobe = sampleWardrobe,
            profile = null
        )

        val initialTrip = tripRepository.getTripById(tripId)!!
        assertFalse(initialTrip.packingList.first().isPacked)

        val firstItem = initialTrip.packingList.first()
        val updatedList = initialTrip.packingList.map {
            if (it.id == firstItem.id) it.copy(isPacked = true) else it
        }
        tripRepository.updateTrip(initialTrip.copy(packingList = updatedList))

        val reloadedTrip = tripRepository.getTripById(tripId)!!
        assertTrue(reloadedTrip.packingList.first { it.id == firstItem.id }.isPacked)
    }

    @Test
    fun `create trip view model validates destination and dates`() {
        val fakeWardrobeRepo = object : WardrobeRepository {
            override val syncMessage: StateFlow<String?> = MutableStateFlow(null)
            override val uploadProgress: StateFlow<Float?> = MutableStateFlow(null)
            override fun getAllClothingItems(): Flow<List<ClothingItem>> = flowOf(sampleWardrobe)
            override fun getClothingItemsByCategory(category: String): Flow<List<ClothingItem>> = flowOf(emptyList())
            override suspend fun addClothingItem(item: ClothingItem): Long = 0L
            override suspend fun updateClothingItem(item: ClothingItem) {}
            override suspend fun deleteClothingItem(id: Long) {}
            override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {}
            override suspend fun persistClothingImage(uri: String): String = ""
        }

        val fakeUserRepo = object : UserRepository {
            override fun getUserProfile(): Flow<UserProfile?> = flowOf(UserProfile())
            override suspend fun saveUserProfile(profile: UserProfile) {}
            override suspend fun resetLearnedPreferences() {}
        }

        val viewModel = CreateTripViewModel(tripRepository, fakeWardrobeRepo, fakeUserRepo)

        // 1. Destination blank validation
        viewModel.updateDestination("   ")
        viewModel.generateTrip { fail("Should not succeed with blank destination") }
        assertEquals("Please enter a travel destination.", viewModel.uiState.value.errorMessage)

        // 2. Return date before departure date validation
        viewModel.updateDestination("New York")
        viewModel.updateDepartureDate("2026-08-10")
        viewModel.updateReturnDate("2026-08-05")
        viewModel.generateTrip { fail("Should not succeed with invalid dates") }
        assertEquals("Return date cannot be earlier than departure date.", viewModel.uiState.value.errorMessage)

        // 3. Valid input
        viewModel.updateReturnDate("2026-08-15")
        var createdId: Long? = null
        viewModel.generateTrip { createdId = it }

        assertNotNull(createdId)
        assertTrue(createdId!! > 0)
    }

    @Test
    fun `trip detail view model supports custom items and set all packed`() = runBlocking {
        val tripId = tripRepository.createTrip(
            destination = "Madrid",
            departureDate = "2026-05-01",
            returnDate = "2026-05-03",
            weatherPreference = "Sunny (20°C)",
            occasion = "Vacation",
            wardrobe = sampleWardrobe,
            profile = null
        )

        val viewModel = TripDetailViewModel(tripRepository, tripId)

        // Add custom item
        viewModel.addCustomPackingItem("Accessories", "Polarized Sunglasses")
        val withCustom = tripRepository.getTripById(tripId)!!
        assertTrue(withCustom.packingList.any { it.itemName == "Polarized Sunglasses" && it.category == "Accessories" })

        // Set all items packed
        viewModel.setAllItemsPacked(true)
        val allPackedTrip = tripRepository.getTripById(tripId)!!
        assertTrue(allPackedTrip.packingList.all { it.isPacked })

        // Set all unpacked
        viewModel.setAllItemsPacked(false)
        val allUnpackedTrip = tripRepository.getTripById(tripId)!!
        assertTrue(allUnpackedTrip.packingList.none { it.isPacked })
    }

    @Test
    fun `repacking alert detects rain forecast and auto-applies missing umbrella and raincoat`() = runBlocking {
        // Fake weather with 75% rain probability and rain weather code (61 = Rain)
        val rainyWeatherRepo = FakeWeatherRepository(rainProbability = 75, weatherCode = 61)
        val rainyTripRepo = TripRepositoryImpl(fakeDao, apiKeyManager, rainyWeatherRepo)

        val tripId = rainyTripRepo.createTrip(
            destination = "Seattle",
            departureDate = "2026-06-01",
            returnDate = "2026-06-02",
            weatherPreference = "Warm",
            occasion = "Vacation",
            wardrobe = sampleWardrobe,
            profile = null
        )

        // Ensure initially packing list has no umbrella or raincoat
        val initialTrip = rainyTripRepo.getTripById(tripId)!!
        val updatedWithoutUmbrella = initialTrip.copy(
            packingList = initialTrip.packingList.filterNot {
                it.itemName.contains("umbrella", true) || it.itemName.contains("waterproof", true)
            }
        )
        rainyTripRepo.updateTrip(updatedWithoutUmbrella)

        // Check repacking alert
        val alert = rainyTripRepo.checkRepackingAlert(tripId)
        assertNotNull(alert)
        assertTrue("Alert should mention rain", alert!!.contains("Rain", ignoreCase = true))

        val tripWithAlert = rainyTripRepo.getTripById(tripId)!!
        assertEquals(alert, tripWithAlert.repackingAlert)

        // Auto-apply recommendations
        val resolvedTrip = rainyTripRepo.autoApplyRepackingRecommendations(tripId)
        assertNotNull(resolvedTrip)
        assertEquals("", resolvedTrip!!.repackingAlert)
        assertTrue(
            "Umbrella should be auto-added to checklist",
            resolvedTrip.packingList.any { it.itemName.contains("umbrella", ignoreCase = true) }
        )
        assertTrue(
            "Water-resistant jacket should be auto-added to checklist",
            resolvedTrip.packingList.any { it.itemName.contains("water-resistant", ignoreCase = true) }
        )
    }

    @Test
    fun `destination geocoding suggestions and live weather update in CreateTripViewModel`() = runBlocking {
        val fakeWardrobeRepo = object : WardrobeRepository {
            override val syncMessage: StateFlow<String?> = MutableStateFlow(null)
            override val uploadProgress: StateFlow<Float?> = MutableStateFlow(null)
            override fun getAllClothingItems(): Flow<List<ClothingItem>> = flowOf(sampleWardrobe)
            override fun getClothingItemsByCategory(category: String): Flow<List<ClothingItem>> = flowOf(emptyList())
            override suspend fun addClothingItem(item: ClothingItem): Long = 0L
            override suspend fun updateClothingItem(item: ClothingItem) {}
            override suspend fun deleteClothingItem(id: Long) {}
            override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {}
            override suspend fun persistClothingImage(uri: String): String = ""
        }

        val fakeUserRepo = object : UserRepository {
            override fun getUserProfile(): Flow<UserProfile?> = flowOf(UserProfile())
            override suspend fun saveUserProfile(profile: UserProfile) {}
            override suspend fun resetLearnedPreferences() {}
        }

        val weatherRepo = FakeWeatherRepository(rainProbability = 10, highC = 23.0, lowC = 16.0)
        val viewModel = CreateTripViewModel(tripRepository, fakeWardrobeRepo, fakeUserRepo, weatherRepo)

        // Select a location suggestion
        val loc = WeatherLocation("Kyoto, Japan", 35.0116, 135.7681, name = "Kyoto", country = "Japan")
        viewModel.selectLocationSuggestion(loc)

        // Wait brief moment for coroutines to complete
        kotlinx.coroutines.delay(100)

        assertEquals("Kyoto", viewModel.uiState.value.destination)
        assertEquals(35.0116, viewModel.uiState.value.destinationLat)
        assertEquals(135.7681, viewModel.uiState.value.destinationLng)
        assertNotNull(viewModel.uiState.value.liveWeatherSummary)
        assertTrue(viewModel.uiState.value.weatherPreference.contains("23°C") || viewModel.uiState.value.weatherPreference.contains("Kyoto"))
    }
}
