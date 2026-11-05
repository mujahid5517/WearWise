package com.example.data.repository

import android.util.Log
import com.example.data.local.ApiKeyManager
import com.example.data.local.dao.TripDao
import com.example.data.local.entity.TripEntity
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateContentRequest
import com.example.data.remote.GeminiPart
import com.example.data.remote.generateTextContent
import com.example.domain.model.ClothingItem
import com.example.domain.model.Trip
import com.example.domain.model.TripDayOutfit
import com.example.domain.model.TripPackingItem
import com.example.domain.model.UserProfile
import com.example.domain.model.WeatherDescriptions
import com.example.domain.model.WeatherLocation
import com.example.domain.repository.TripRepository
import com.example.domain.repository.WeatherRepository
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TripRepositoryImpl(
    private val tripDao: TripDao,
    private val apiKeyManager: ApiKeyManager,
    private val weatherRepository: WeatherRepository? = null
) : TripRepository {

    private val moshi = GeminiClient.moshi
    private val packingListAdapter = moshi.adapter<List<TripPackingItem>>(
        Types.newParameterizedType(List::class.java, TripPackingItem::class.java)
    )
    private val itineraryAdapter = moshi.adapter<List<TripDayOutfit>>(
        Types.newParameterizedType(List::class.java, TripDayOutfit::class.java)
    )

    override fun observeAllTrips(): Flow<List<Trip>> {
        return tripDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTripById(id: Long): Trip? {
        return tripDao.getById(id)?.toDomain()
    }

    override suspend fun createTrip(
        destination: String,
        departureDate: String,
        returnDate: String,
        weatherPreference: String,
        occasion: String,
        wardrobe: List<ClothingItem>,
        profile: UserProfile?,
        destinationLat: Double?,
        destinationLng: Double?,
        originLat: Double?,
        originLng: Double?,
        transportMode: String
    ): Long {
        val durationDays = calculateDurationDays(departureDate, returnDate)
        val availableWardrobe = wardrobe.filter { it.isAvailableForTrip() }

        // Geocode coordinates if not provided and weatherRepository is present
        var finalLat = destinationLat
        var finalLng = destinationLng
        var liveForecastSnippet = ""

        if (weatherRepository != null) {
            try {
                if (finalLat == null || finalLng == null) {
                    val searchResults = weatherRepository.searchLocations(destination)
                    val matched = searchResults.firstOrNull()
                    if (matched != null) {
                        finalLat = matched.latitude
                        finalLng = matched.longitude
                    }
                }

                if (finalLat != null && finalLng != null) {
                    val forecast = weatherRepository.refresh(WeatherLocation(destination, finalLat, finalLng))
                    val dailyMap = forecast.daily.associateBy { it.date }
                    val forecastLines = mutableListOf<String>()

                    for (dayNum in 1..durationDays) {
                        val dateStr = runCatching {
                            LocalDate.parse(departureDate.trim()).plusDays((dayNum - 1).toLong()).toString()
                        }.getOrNull()

                        val dayWeather = if (dateStr != null) dailyMap[dateStr] else null
                        if (dayWeather != null) {
                            val desc = WeatherDescriptions.describe(dayWeather.weatherCode)
                            val rain = dayWeather.precipitationProbability?.let { "$it% rain chance" } ?: ""
                            val temp = dayWeather.highC?.let { "${it.toInt()}°C" } ?: ""
                            forecastLines.add("Day $dayNum ($dateStr): $temp $desc $rain".trim())
                        }
                    }

                    if (forecastLines.isNotEmpty()) {
                        liveForecastSnippet = "LIVE WEATHER FORECAST:\n" + forecastLines.joinToString("\n")
                    }
                }
            } catch (e: Exception) {
                Log.w("TripRepository", "Could not fetch live weather during trip creation: ${e.message}")
            }
        }

        val effectiveWeather = if (liveForecastSnippet.isNotBlank() && weatherPreference.isBlank()) {
            "Live Destination Forecast"
        } else {
            weatherPreference
        }

        val apiKey = apiKeyManager.getEffectiveApiKey()
        var packingList: List<TripPackingItem>
        var itinerary: List<TripDayOutfit>

        if (apiKey.isNotBlank()) {
            try {
                val wardrobeSummary = if (availableWardrobe.isNotEmpty()) {
                    availableWardrobe.joinToString("\n") {
                        "- [ID:${it.id}] ${it.category}: ${it.name} (${it.color}, ${it.style}, ${it.formality})"
                    }
                } else {
                    "No wardrobe items available locally."
                }

                val prompt = """
                Act as an expert AI travel stylist and packing assistant for the WearWise app.
                Create a comprehensive packing list and day-by-day outfit itinerary for a trip.
                
                TRIP DETAILS:
                - Destination: $destination
                - Departure Date: $departureDate
                - Return Date: $returnDate
                - Duration: $durationDays days
                - Expected Weather / Climate: $effectiveWeather
                - Travel Occasion: $occasion
                - User Style Preference: ${profile?.stylePreferences?.joinToString() ?: "Casual Chic"}

                ${if (liveForecastSnippet.isNotBlank()) liveForecastSnippet else ""}

                AVAILABLE WARDROBE ITEMS (Only recommend from these available items; do NOT recommend unavailable or laundry clothing):
                $wardrobeSummary

                REQUIREMENTS:
                1. "packingList": Array of items to pack, categorized into exactly these categories: "Tops", "Bottoms", "Shoes", "Accessories", "Essentials".
                   - For wardrobe clothing items, include "itemName", "category", and "clothingItemId" matching the ID.
                   - For non-clothing travel necessities (passport, charger, toiletries, sunscreen, medication, underwear/socks, compact umbrella if rain is forecasted), include them under category "Essentials" with "clothingItemId": null.
                2. "itinerary": Array of day-by-day outfit combinations for all $durationDays travel days (dayNumber 1 to $durationDays).
                   - Each day must include: "dayNumber" (Int), "dateStr" (e.g. "$departureDate" or "Day 1"), "outfitTitle" (e.g. "Sightseeing & Arrival Comfort"), "rationale" (why suitable for weather and occasion), and "clothingItemNames" (Array of garment names forming a complete outfit).

                Return your response in pure JSON format:
                {
                  "packingList": [
                    {"category": "Tops", "itemName": "White Oxford Cotton", "clothingItemId": 1},
                    {"category": "Essentials", "itemName": "Passport & Travel Documents", "clothingItemId": null}
                  ],
                  "itinerary": [
                    {
                      "dayNumber": 1,
                      "dateStr": "$departureDate",
                      "outfitTitle": "Arrival & City Stroll",
                      "rationale": "Light breathable layers suited for arrival.",
                      "clothingItemNames": ["White Oxford Cotton", "Navy Tailored Chinos"]
                    }
                  ]
                }
                """.trimIndent()

                val request = GeminiGenerateContentRequest(
                    contents = listOf(GeminiContent(listOf(GeminiPart(text = prompt))))
                )

                val response = GeminiClient.apiService.generateTextContent(apiKey, request)
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim().orEmpty()

                val parsed = parseAiTripResponse(reply, availableWardrobe, durationDays, departureDate, destination, occasion, transportMode)
                packingList = parsed.first
                itinerary = parsed.second
            } catch (e: Exception) {
                Log.w("TripRepository", "Gemini AI trip generation failed, using intelligent heuristic fallback: ${e.message}")
                val fallback = generateFallbackTrip(destination, departureDate, durationDays, occasion, effectiveWeather, availableWardrobe, transportMode)
                packingList = fallback.first
                itinerary = fallback.second
            }
        } else {
            Log.d("TripRepository", "No Gemini API key available; using heuristic travel styling.")
            val fallback = generateFallbackTrip(destination, departureDate, durationDays, occasion, effectiveWeather, availableWardrobe, transportMode)
            packingList = fallback.first
            itinerary = fallback.second
        }

        val finalOriginLat = originLat ?: (finalLat?.let { it - 3.2 } ?: 9.03)
        val finalOriginLng = originLng ?: (finalLng ?: 38.74)
        val (distKm, durHours) = calculateTravelMetrics(finalOriginLat, finalOriginLng, finalLat, finalLng, transportMode)

        val entity = TripEntity(
            destination = destination,
            departureDate = departureDate,
            returnDate = returnDate,
            weatherPreference = effectiveWeather,
            occasion = occasion,
            packingListJson = packingListAdapter.toJson(packingList),
            itineraryJson = itineraryAdapter.toJson(itinerary),
            ownerUid = profile?.email ?: "",
            createdAt = System.currentTimeMillis(),
            destinationLat = finalLat,
            destinationLng = finalLng,
            repackingAlert = "",
            originLat = finalOriginLat,
            originLng = finalOriginLng,
            travelDistanceKm = distKm,
            travelDurationHours = durHours,
            transportMode = transportMode
        )

        return tripDao.insert(entity)
    }

    override suspend fun updateTrip(trip: Trip) {
        val (distKm, durHours) = calculateTravelMetrics(trip.originLat, trip.originLng, trip.destinationLat, trip.destinationLng, trip.transportMode)
        val entity = TripEntity(
            id = trip.id,
            destination = trip.destination,
            departureDate = trip.departureDate,
            returnDate = trip.returnDate,
            weatherPreference = trip.weatherPreference,
            occasion = trip.occasion,
            packingListJson = packingListAdapter.toJson(trip.packingList),
            itineraryJson = itineraryAdapter.toJson(trip.itinerary),
            ownerUid = trip.ownerUid,
            createdAt = trip.createdAt,
            destinationLat = trip.destinationLat,
            destinationLng = trip.destinationLng,
            repackingAlert = trip.repackingAlert,
            originLat = trip.originLat,
            originLng = trip.originLng,
            travelDistanceKm = distKm ?: trip.travelDistanceKm,
            travelDurationHours = durHours ?: trip.travelDurationHours,
            transportMode = trip.transportMode
        )
        tripDao.insert(entity)
    }

    override suspend fun deleteTrip(id: Long) {
        tripDao.delete(id)
    }

    override suspend fun checkRepackingAlert(tripId: Long): String? {
        val trip = getTripById(tripId) ?: return null
        if (weatherRepository == null) return null

        try {
            var lat = trip.destinationLat
            var lng = trip.destinationLng

            if (lat == null || lng == null) {
                val loc = weatherRepository.searchLocations(trip.destination).firstOrNull()
                if (loc != null) {
                    lat = loc.latitude
                    lng = loc.longitude
                    updateTrip(trip.copy(destinationLat = lat, destinationLng = lng))
                }
            }

            if (lat == null || lng == null) return null

            val forecast = weatherRepository.refresh(WeatherLocation(trip.destination, lat, lng))
            val start = runCatching { LocalDate.parse(trip.departureDate.trim()) }.getOrNull()
            val end = runCatching { LocalDate.parse(trip.returnDate.trim()) }.getOrNull()

            val relevantDaily = forecast.daily.filter { day ->
                val date = runCatching { LocalDate.parse(day.date) }.getOrNull()
                if (start != null && end != null && date != null) {
                    !date.isBefore(start) && !date.isAfter(end)
                } else true
            }

            val itemNamesLower = trip.packingList.map { it.itemName.lowercase() }
            var alertMessage: String? = null

            // 1. Rain check
            val rainDay = relevantDaily.firstOrNull { day ->
                (day.precipitationProbability ?: 0) >= 45 ||
                        listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95).contains(day.weatherCode)
            }
            if (rainDay != null) {
                val hasRainGear = itemNamesLower.any {
                    it.contains("umbrella") || it.contains("raincoat") || it.contains("waterproof") || it.contains("rain")
                }
                if (!hasRainGear) {
                    val prob = rainDay.precipitationProbability ?: 60
                    alertMessage = "🌧️ Weather Shift: Rain forecasted in ${trip.destination} on ${rainDay.date} ($prob% chance). You don't have rain gear packed."
                }
            }

            // 2. Cold snap check
            if (alertMessage == null) {
                val coldDay = relevantDaily.firstOrNull { (it.highC ?: 20.0) < 14.0 || (it.lowC ?: 15.0) < 9.0 }
                if (coldDay != null) {
                    val hasWarmLayer = itemNamesLower.any {
                        it.contains("jacket") || it.contains("coat") || it.contains("sweater") || it.contains("fleece") || it.contains("blazer")
                    }
                    if (!hasWarmLayer) {
                        val temp = (coldDay.lowC ?: coldDay.highC)?.toInt() ?: 10
                        alertMessage = "❄️ Weather Shift: Chilly temperatures forecast in ${trip.destination} (low of ${temp}°C). Consider packing a warm jacket or sweater."
                    }
                }
            }

            // 3. Heat wave check
            if (alertMessage == null) {
                val hotDay = relevantDaily.firstOrNull { (it.highC ?: 20.0) >= 28.0 }
                if (hotDay != null) {
                    val hasSunGear = itemNamesLower.any {
                        it.contains("sunglasses") || it.contains("sunscreen") || it.contains("hat") || it.contains("cap")
                    }
                    if (!hasSunGear) {
                        val temp = hotDay.highC?.toInt() ?: 29
                        alertMessage = "☀️ Weather Shift: High heat expected in ${trip.destination} (${temp}°C). Don't forget sunscreen and sunglasses."
                    }
                }
            }

            if (trip.repackingAlert != (alertMessage ?: "")) {
                updateTrip(trip.copy(repackingAlert = alertMessage ?: ""))
            }

            return alertMessage
        } catch (e: Exception) {
            Log.w("TripRepository", "checkRepackingAlert failed: ${e.message}")
            return null
        }
    }

    override suspend fun autoApplyRepackingRecommendations(tripId: Long): Trip? {
        val trip = getTripById(tripId) ?: return null
        val alert = trip.repackingAlert
        if (alert.isBlank()) return trip

        val newItems = mutableListOf<TripPackingItem>()
        val existingNames = trip.packingList.map { it.itemName.lowercase() }

        when {
            alert.contains("🌧️") || alert.contains("Rain") -> {
                if (!existingNames.any { it.contains("umbrella") }) {
                    newItems.add(TripPackingItem(category = "Essentials", itemName = "Compact Travel Umbrella"))
                }
                if (!existingNames.any { it.contains("waterproof") || it.contains("raincoat") }) {
                    newItems.add(TripPackingItem(category = "Tops", itemName = "Water-Resistant Light Jacket"))
                }
            }
            alert.contains("❄️") || alert.contains("Chilly") -> {
                if (!existingNames.any { it.contains("jacket") || it.contains("coat") || it.contains("sweater") }) {
                    newItems.add(TripPackingItem(category = "Tops", itemName = "Warm Layer / Insulated Jacket"))
                }
            }
            alert.contains("☀️") || alert.contains("heat") -> {
                if (!existingNames.any { it.contains("sunscreen") }) {
                    newItems.add(TripPackingItem(category = "Essentials", itemName = "UV Broad Spectrum Sunscreen"))
                }
                if (!existingNames.any { it.contains("sunglasses") }) {
                    newItems.add(TripPackingItem(category = "Accessories", itemName = "Polarized Sunglasses"))
                }
            }
        }

        val updatedTrip = trip.copy(
            packingList = trip.packingList + newItems,
            repackingAlert = ""
        )
        updateTrip(updatedTrip)
        return updatedTrip
    }

    private fun TripEntity.toDomain(): Trip {
        val packingList = runCatching { packingListAdapter.fromJson(packingListJson) }.getOrNull() ?: emptyList()
        val itinerary = runCatching { itineraryAdapter.fromJson(itineraryJson) }.getOrNull() ?: emptyList()
        return Trip(
            id = id,
            destination = destination,
            departureDate = departureDate,
            returnDate = returnDate,
            weatherPreference = weatherPreference,
            occasion = occasion,
            packingList = packingList,
            itinerary = itinerary,
            ownerUid = ownerUid,
            createdAt = createdAt,
            destinationLat = destinationLat,
            destinationLng = destinationLng,
            repackingAlert = repackingAlert,
            originLat = originLat,
            originLng = originLng,
            travelDistanceKm = travelDistanceKm,
            travelDurationHours = travelDurationHours,
            transportMode = transportMode
        )
    }

    private fun calculateTravelMetrics(
        originLat: Double?,
        originLng: Double?,
        destLat: Double?,
        destLng: Double?,
        transportMode: String = "Car"
    ): Pair<Double?, Double?> {
        if (originLat == null || originLng == null || destLat == null || destLng == null) {
            return Pair(null, null)
        }
        val r = 6371.0
        val dLat = Math.toRadians(destLat - originLat)
        val dLon = Math.toRadians(destLng - originLng)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(originLat)) * Math.cos(Math.toRadians(destLat)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val straightDistKm = r * c

        val isFlight = transportMode.equals("Flight", ignoreCase = true)
        val distanceKm = if (!isFlight && straightDistKm < 1000.0) straightDistKm * 1.25 else straightDistKm

        val durationHours = when {
            transportMode.equals("Flight", ignoreCase = true) -> {
                (distanceKm / 750.0) + 2.0
            }
            transportMode.equals("Train", ignoreCase = true) -> {
                (distanceKm / 110.0) + 0.3
            }
            transportMode.equals("Bus", ignoreCase = true) -> {
                (distanceKm / 50.0) + 0.5
            }
            else -> {
                // Car / Driving
                (distanceKm / 65.0) + 0.5
            }
        }

        val roundedDist = kotlin.math.round(distanceKm * 10) / 10.0
        val roundedDuration = kotlin.math.round(durationHours * 10) / 10.0
        return Pair(roundedDist, roundedDuration)
    }

    private fun parseAiTripResponse(
        reply: String,
        availableWardrobe: List<ClothingItem>,
        durationDays: Int,
        departureDate: String,
        destination: String,
        occasion: String,
        transportMode: String = "Car"
    ): Pair<List<TripPackingItem>, List<TripDayOutfit>> {
        val json = reply.substringAfter("```json", reply)
            .substringAfter("```", reply)
            .substringBefore("```")
            .trim()

        val root = runCatching { moshi.adapter(Map::class.java).fromJson(json) }.getOrNull()
            ?: return generateFallbackTrip(destination, departureDate, durationDays, occasion, "", availableWardrobe, transportMode)

        val allowedById = availableWardrobe.associateBy { it.id }
        val allowedByName = availableWardrobe.associateBy { it.name.trim().lowercase() }

        val packingListRaw = (root["packingList"] as? List<*>).orEmpty().mapNotNull { row ->
            val values = row as? Map<*, *> ?: return@mapNotNull null
            val name = values["itemName"]?.toString()?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null

            val categoryRaw = values["category"]?.toString()
            val rawId = (values["clothingItemId"] as? Number)?.toLong()
            val matchedItem = rawId?.let(allowedById::get) ?: allowedByName[name.lowercase()]

            if (matchedItem != null) {
                TripPackingItem(
                    category = normalizeCategory(matchedItem.category),
                    itemName = matchedItem.name,
                    clothingItemId = matchedItem.id
                )
            } else {
                TripPackingItem(
                    category = normalizeCategory(categoryRaw ?: "Essentials"),
                    itemName = name,
                    clothingItemId = null
                )
            }
        }.distinctBy { item ->
            item.clothingItemId?.toString() ?: item.itemName.trim().lowercase()
        }.toMutableList()

        val existingNames = packingListRaw.map { it.itemName.lowercase() }.toSet()
        val defaultEssentials = listOf(
            "Passport & Travel Documents",
            "Phone Charger & Power Bank",
            "Toiletries & Grooming Kit",
            "Underwear & Socks ($durationDays pairs)",
            "Sunscreen & Lip Balm",
            "Universal Travel Adapter"
        )
        for (essential in defaultEssentials) {
            if (!existingNames.any { it.contains(essential.substringBefore("&").trim().lowercase()) }) {
                packingListRaw.add(
                    TripPackingItem(
                        category = "Essentials",
                        itemName = essential,
                        clothingItemId = null
                    )
                )
            }
        }

        val parsedOutfits = (root["itinerary"] as? List<*>).orEmpty().mapNotNull { row ->
            val values = row as? Map<*, *> ?: return@mapNotNull null
            val day = (values["dayNumber"] as? Number)?.toInt() ?: return@mapNotNull null
            val date = values["dateStr"]?.toString()?.ifBlank { "Day $day" } ?: "Day $day"
            val title = values["outfitTitle"]?.toString()?.ifBlank { "Day $day in $destination" } ?: "Day $day in $destination"
            val rationale = values["rationale"]?.toString()?.ifBlank { "Curated travel combination." } ?: "Curated travel combination."

            val itemNames = (values["clothingItemNames"] as? List<*>).orEmpty().mapNotNull { rawName ->
                val str = rawName?.toString()?.trim().orEmpty()
                if (str.isBlank()) null else {
                    allowedByName[str.lowercase()]?.name ?: str
                }
            }

            TripDayOutfit(
                dayNumber = day,
                dateStr = date,
                outfitTitle = title,
                rationale = rationale,
                clothingItemNames = itemNames
            )
        }.toMutableList()

        val completedOutfits = ensureCompleteItinerary(
            parsedOutfits,
            durationDays,
            departureDate,
            destination,
            availableWardrobe,
            packingListRaw
        )

        return packingListRaw to completedOutfits
    }

    private fun normalizeCategory(category: String?): String {
        val s = category?.trim()?.lowercase().orEmpty()
        return when {
            s.contains("top") || s.contains("shirt") || s.contains("sweater") || s.contains("jacket") || s.contains("blazer") || s.contains("outerwear") || s.contains("knitwear") -> "Tops"
            s.contains("bottom") || s.contains("pant") || s.contains("jean") || s.contains("trouser") || s.contains("short") || s.contains("skirt") -> "Bottoms"
            s.contains("shoe") || s.contains("boot") || s.contains("sneaker") || s.contains("loafer") || s.contains("footwear") || s.contains("sandal") -> "Shoes"
            s.contains("access") || s.contains("watch") || s.contains("bag") || s.contains("belt") || s.contains("hat") || s.contains("sunglass") -> "Accessories"
            else -> "Essentials"
        }
    }

    private fun ClothingItem.isAvailableForTrip(): Boolean {
        val texts = listOf(description, occasion, season, style, formality, subcategory, name)
        return !texts.any { it.contains("unavailable", ignoreCase = true) || it.contains("laundry", ignoreCase = true) }
    }

    private fun calculateDurationDays(departureDate: String, returnDate: String): Int {
        return runCatching {
            val start = LocalDate.parse(departureDate.trim())
            val end = LocalDate.parse(returnDate.trim())
            (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceIn(1, 30)
        }.getOrDefault(3)
    }

    private fun generateFallbackTrip(
        destination: String,
        departureDate: String,
        durationDays: Int,
        occasion: String,
        weatherPreference: String,
        availableWardrobe: List<ClothingItem>,
        transportMode: String = "Car"
    ): Pair<List<TripPackingItem>, List<TripDayOutfit>> {
        val packing = mutableListOf<TripPackingItem>()

        val tops = availableWardrobe.filter { normalizeCategory(it.category) == "Tops" }
        val bottoms = availableWardrobe.filter { normalizeCategory(it.category) == "Bottoms" }
        val shoes = availableWardrobe.filter { normalizeCategory(it.category) == "Shoes" }
        val accessories = availableWardrobe.filter { normalizeCategory(it.category) == "Accessories" }

        val topsToTake = tops.take((durationDays + 1).coerceIn(2, 7))
        val bottomsToTake = bottoms.take(((durationDays / 2) + 1).coerceIn(2, 4))
        val shoesToTake = shoes.take(2.coerceAtMost(shoes.size.coerceAtLeast(1)))
        val accessoriesToTake = accessories.take(3)

        (topsToTake + bottomsToTake + shoesToTake + accessoriesToTake).forEach { item ->
            packing.add(
                TripPackingItem(
                    category = normalizeCategory(item.category),
                    itemName = item.name,
                    clothingItemId = item.id
                )
            )
        }

        val essentials = listOf(
            "Passport & Travel Documents",
            "Phone Charger & Power Bank",
            "Toiletries & Grooming Kit",
            "Underwear & Socks ($durationDays sets)",
            "Sunscreen & Lip Balm",
            "Universal Travel Adapter",
            "Reusable Water Bottle"
        )
        essentials.forEach { essential ->
            packing.add(TripPackingItem(category = "Essentials", itemName = essential, clothingItemId = null))
        }

        // If weather mentions rain, auto-include umbrella in fallback
        if (weatherPreference.contains("rain", ignoreCase = true) || weatherPreference.contains("shower", ignoreCase = true)) {
            packing.add(TripPackingItem(category = "Essentials", itemName = "Compact Travel Umbrella", clothingItemId = null))
        }

        when {
            transportMode.equals("Flight", ignoreCase = true) -> {
                packing.add(TripPackingItem(category = "Essentials", itemName = "Compression Socks & Cabin Layer", clothingItemId = null))
            }
            transportMode.equals("Bus", ignoreCase = true) -> {
                packing.add(TripPackingItem(category = "Essentials", itemName = "Coach Travel Cushion & Water Bottle", clothingItemId = null))
            }
            transportMode.equals("Train", ignoreCase = true) -> {
                packing.add(TripPackingItem(category = "Essentials", itemName = "Rail Travel Light Scarf", clothingItemId = null))
            }
            else -> {
                packing.add(TripPackingItem(category = "Essentials", itemName = "Road Trip Sunglasses & Comfort Layers", clothingItemId = null))
            }
        }

        val outfits = (1..durationDays).map { dayNum ->
            val dateStr = runCatching {
                LocalDate.parse(departureDate.trim()).plusDays((dayNum - 1).toLong()).toString()
            }.getOrDefault(if (dayNum == 1) departureDate else "Day $dayNum")

            val top = tops.getOrNull((dayNum - 1) % tops.size.coerceAtLeast(1))?.name
                ?: "Casual Travel Shirt"
            val bottom = bottoms.getOrNull((dayNum - 1) % bottoms.size.coerceAtLeast(1))?.name
                ?: "Tailored Trousers"
            val shoe = shoes.getOrNull((dayNum - 1) % shoes.size.coerceAtLeast(1))?.name
                ?: "Comfort Walking Shoes"
            val accessory = accessories.getOrNull((dayNum - 1) % accessories.size.coerceAtLeast(1))?.name

            val outfitItems = listOfNotNull(top, bottom, shoe, accessory)

            TripDayOutfit(
                dayNumber = dayNum,
                dateStr = dateStr,
                outfitTitle = "Day $dayNum — $occasion in $destination",
                rationale = "Harmonious combination selected for $weatherPreference and comfort.",
                clothingItemNames = outfitItems
            )
        }

        return packing to outfits
    }

    private fun ensureCompleteItinerary(
        existingOutfits: List<TripDayOutfit>,
        durationDays: Int,
        departureDate: String,
        destination: String,
        availableWardrobe: List<ClothingItem>,
        packingList: List<TripPackingItem>
    ): List<TripDayOutfit> {
        val byDay = existingOutfits.associateBy { it.dayNumber }.toMutableMap()

        val tops = packingList.filter { it.category == "Tops" }.map { it.itemName }
        val bottoms = packingList.filter { it.category == "Bottoms" }.map { it.itemName }
        val shoes = packingList.filter { it.category == "Shoes" }.map { it.itemName }

        for (day in 1..durationDays) {
            val dateStr = runCatching {
                LocalDate.parse(departureDate.trim()).plusDays((day - 1).toLong()).toString()
            }.getOrDefault(if (day == 1) departureDate else "Day $day")

            val current = byDay[day]
            if (current == null || current.clothingItemNames.isEmpty()) {
                val dayTop = tops.getOrNull((day - 1) % tops.size.coerceAtLeast(1))
                    ?: availableWardrobe.firstOrNull { normalizeCategory(it.category) == "Tops" }?.name
                    ?: "Travel Top"
                val dayBottom = bottoms.getOrNull((day - 1) % bottoms.size.coerceAtLeast(1))
                    ?: availableWardrobe.firstOrNull { normalizeCategory(it.category) == "Bottoms" }?.name
                    ?: "Travel Pants"
                val dayShoe = shoes.getOrNull((day - 1) % shoes.size.coerceAtLeast(1))
                    ?: availableWardrobe.firstOrNull { normalizeCategory(it.category) == "Shoes" }?.name
                    ?: "Comfortable Footwear"

                byDay[day] = TripDayOutfit(
                    dayNumber = day,
                    dateStr = dateStr,
                    outfitTitle = current?.outfitTitle?.ifBlank { "Day $day in $destination" } ?: "Day $day in $destination",
                    rationale = current?.rationale?.ifBlank { "Complete outfit tailored for day $day." } ?: "Complete outfit tailored for day $day.",
                    clothingItemNames = listOf(dayTop, dayBottom, dayShoe)
                )
            }
        }

        return (1..durationDays).mapNotNull { byDay[it] }
    }
}
