package com.example.presentation.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.WeatherDescriptions
import com.example.domain.model.WeatherLocation
import com.example.domain.repository.TripRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WardrobeRepository
import com.example.domain.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CreateTripUiState(
    val destination: String = "",
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    val origin: String = "",
    val originLat: Double? = null,
    val originLng: Double? = null,
    val originSuggestions: List<WeatherLocation> = emptyList(),
    val departureDate: String = defaultDepartureDate(),
    val returnDate: String = defaultReturnDate(),
    val weatherPreference: String = "Sunny & Warm (24°C)",
    val occasion: String = "Vacation / Leisure",
    val transportMode: String = "Car",
    val locationSuggestions: List<WeatherLocation> = emptyList(),
    val isFetchingWeather: Boolean = false,
    val liveWeatherSummary: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdTripId: Long? = null
) {
    val durationDays: Int
        get() = runCatching {
            val start = LocalDate.parse(departureDate.trim())
            val end = LocalDate.parse(returnDate.trim())
            (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceIn(1, 30)
        }.getOrDefault(1)
}

private fun defaultDepartureDate(): String = runCatching {
    LocalDate.now().toString()
}.getOrDefault("2026-06-01")

private fun defaultReturnDate(): String = runCatching {
    LocalDate.now().plusDays(4).toString()
}.getOrDefault("2026-06-05")

class CreateTripViewModel(
    private val tripRepository: TripRepository,
    private val wardrobeRepository: WardrobeRepository,
    private val userRepository: UserRepository,
    private val weatherRepository: WeatherRepository? = null
) : ViewModel() {

    companion object {
        val WEATHER_OPTIONS = listOf(
            "Sunny & Warm (24°C)",
            "Cool & Breezy (16°C)",
            "Cold & Winter (2°C)",
            "Rainy & Mild (14°C)",
            "Tropical & Humid (30°C)"
        )

        val OCCASION_OPTIONS = listOf(
            "Vacation / Leisure",
            "Business Conference",
            "Weekend Getaway",
            "Wedding / Formal Event",
            "Active / Outdoor Adventure"
        )

        val TRANSPORT_MODE_OPTIONS = listOf("Car", "Bus", "Train", "Flight")
    }

    private val _uiState = MutableStateFlow(CreateTripUiState())
    val uiState: StateFlow<CreateTripUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateDestination(value: String) {
        _uiState.update { it.copy(destination = value) }

        if (weatherRepository == null) return

        searchJob?.cancel()
        val trimmed = value.trim()
        if (trimmed.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(300) // Debounce search
                try {
                    val results = weatherRepository.searchLocations(trimmed)
                    _uiState.update { it.copy(locationSuggestions = results) }
                } catch (_: Exception) {
                    _uiState.update { it.copy(locationSuggestions = emptyList()) }
                }
            }
        } else {
            _uiState.update { it.copy(locationSuggestions = emptyList()) }
        }
    }

    fun selectLocationSuggestion(location: WeatherLocation) {
        _uiState.update {
            it.copy(
                destination = location.name,
                destinationLat = location.latitude,
                destinationLng = location.longitude,
                locationSuggestions = emptyList()
            )
        }
        fetchLiveWeather(location.name, location.latitude, location.longitude)
    }

    private var originSearchJob: Job? = null

    fun updateOrigin(value: String) {
        _uiState.update { it.copy(origin = value) }
        if (weatherRepository == null) return

        originSearchJob?.cancel()
        val trimmed = value.trim()
        if (trimmed.length >= 2) {
            originSearchJob = viewModelScope.launch {
                delay(300)
                try {
                    val results = weatherRepository.searchLocations(trimmed)
                    _uiState.update { it.copy(originSuggestions = results) }
                } catch (_: Exception) {
                    _uiState.update { it.copy(originSuggestions = emptyList()) }
                }
            }
        } else {
            _uiState.update { it.copy(originSuggestions = emptyList()) }
        }
    }

    fun selectOriginSuggestion(location: WeatherLocation) {
        _uiState.update {
            it.copy(
                origin = location.name,
                originLat = location.latitude,
                originLng = location.longitude,
                originSuggestions = emptyList()
            )
        }
    }

    fun fetchLiveWeatherForCurrentDestination() {
        val state = _uiState.value
        val dest = state.destination.trim()
        if (dest.isBlank() || weatherRepository == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingWeather = true) }
            try {
                var lat = state.destinationLat
                var lng = state.destinationLng

                if (lat == null || lng == null) {
                    val found = weatherRepository.searchLocations(dest).firstOrNull()
                    if (found != null) {
                        lat = found.latitude
                        lng = found.longitude
                        _uiState.update { it.copy(destinationLat = lat, destinationLng = lng) }
                    }
                }

                if (lat != null && lng != null) {
                    fetchLiveWeather(dest, lat, lng)
                } else {
                    _uiState.update {
                        it.copy(
                            isFetchingWeather = false,
                            errorMessage = "Could not find geographic coordinates for '$dest'."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingWeather = false,
                        errorMessage = "Could not fetch destination weather: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun fetchLiveWeather(destination: String, lat: Double, lng: Double) {
        if (weatherRepository == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingWeather = true) }
            try {
                val forecast = weatherRepository.refresh(WeatherLocation(destination, lat, lng))
                val state = _uiState.value
                val start = runCatching { LocalDate.parse(state.departureDate.trim()) }.getOrNull()
                val end = runCatching { LocalDate.parse(state.returnDate.trim()) }.getOrNull()

                val matchedDays = forecast.daily.filter { day ->
                    val date = runCatching { LocalDate.parse(day.date) }.getOrNull()
                    if (start != null && end != null && date != null) {
                        !date.isBefore(start) && !date.isAfter(end)
                    } else true
                }

                if (matchedDays.isNotEmpty()) {
                    val maxTemp = matchedDays.mapNotNull { it.highC }.maxOrNull()?.toInt()
                    val minTemp = matchedDays.mapNotNull { it.lowC }.minOrNull()?.toInt()
                    val maxRainProb = matchedDays.mapNotNull { it.precipitationProbability }.maxOrNull() ?: 0
                    val mostCommonCode = matchedDays.mapNotNull { it.weatherCode }.groupBy { it }
                        .maxByOrNull { it.value.size }?.key

                    val condition = WeatherDescriptions.describe(mostCommonCode)
                    val summary = buildString {
                        if (minTemp != null && maxTemp != null) {
                            append("${minTemp}°C–${maxTemp}°C · ")
                        }
                        append(condition)
                        if (maxRainProb > 20) {
                            append(" · ${maxRainProb}% rain chance")
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isFetchingWeather = false,
                            weatherPreference = summary,
                            liveWeatherSummary = summary
                        )
                    }
                } else {
                    val currentTemp = forecast.temperatureC?.toInt() ?: 20
                    val currentCondition = WeatherDescriptions.describe(forecast.weatherCode)
                    val summary = "${currentTemp}°C · $currentCondition"
                    _uiState.update {
                        it.copy(
                            isFetchingWeather = false,
                            weatherPreference = summary,
                            liveWeatherSummary = summary
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingWeather = false,
                        errorMessage = "Live forecast unavailable: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun updateDepartureDate(value: String) {
        _uiState.update { it.copy(departureDate = value) }
        recheckLiveWeather()
    }

    fun updateReturnDate(value: String) {
        _uiState.update { it.copy(returnDate = value) }
        recheckLiveWeather()
    }

    private fun recheckLiveWeather() {
        val state = _uiState.value
        val lat = state.destinationLat
        val lng = state.destinationLng
        if (lat != null && lng != null && state.destination.isNotBlank()) {
            fetchLiveWeather(state.destination, lat, lng)
        }
    }

    fun updateTransportMode(value: String) {
        _uiState.update { it.copy(transportMode = value) }
    }

    fun updateWeatherPreference(value: String) {
        _uiState.update { it.copy(weatherPreference = value) }
    }

    fun updateOccasion(value: String) {
        _uiState.update { it.copy(occasion = value) }
    }

    fun generateTrip(onSuccess: (Long) -> Unit) {
        val state = _uiState.value
        if (state.destination.trim().isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a travel destination.") }
            return
        }

        val start = runCatching { LocalDate.parse(state.departureDate.trim()) }.getOrNull()
        val end = runCatching { LocalDate.parse(state.returnDate.trim()) }.getOrNull()

        if (start != null && end != null && end.isBefore(start)) {
            _uiState.update { it.copy(errorMessage = "Return date cannot be earlier than departure date.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val wardrobe = wardrobeRepository.getAllClothingItems().first()
                val profile = userRepository.getUserProfile().first()

                val tripId = tripRepository.createTrip(
                    destination = state.destination.trim(),
                    departureDate = state.departureDate.trim(),
                    returnDate = state.returnDate.trim(),
                    weatherPreference = state.weatherPreference.trim(),
                    occasion = state.occasion.trim(),
                    wardrobe = wardrobe,
                    profile = profile,
                    destinationLat = state.destinationLat,
                    destinationLng = state.destinationLng,
                    originLat = state.originLat,
                    originLng = state.originLng,
                    transportMode = state.transportMode
                )

                _uiState.update { it.copy(isLoading = false, createdTripId = tripId) }
                onSuccess(tripId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate packing list: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
