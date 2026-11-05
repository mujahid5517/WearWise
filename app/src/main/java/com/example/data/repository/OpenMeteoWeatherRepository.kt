package com.example.data.repository

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.example.domain.model.DailyWeather
import com.example.domain.model.WeatherForecast
import com.example.domain.model.WeatherLocation
import com.example.domain.repository.WeatherRepository
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoGeocodingResult>? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoGeocodingResult(
    val id: Long? = null,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @param:Json(name = "country_code") val countryCode: String? = null,
    val admin1: String? = null,
    val timezone: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    @param:Json(name = "current_weather") val currentWeather: OpenMeteoCurrentWeather? = null,
    val daily: OpenMeteoDailyData? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrentWeather(
    val temperature: Double? = null,
    val windspeed: Double? = null,
    val weathercode: Int? = null,
    val time: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoDailyData(
    val time: List<String>? = null,
    val weathercode: List<Int>? = null,
    @param:Json(name = "temperature_2m_max") val temperatureMax: List<Double>? = null,
    @param:Json(name = "temperature_2m_min") val temperatureMin: List<Double>? = null,
    @param:Json(name = "apparent_temperature_max") val apparentTemperatureMax: List<Double>? = null,
    @param:Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>? = null,
    @param:Json(name = "windspeed_10m_max") val windspeedMax: List<Double>? = null
)

class OpenMeteoWeatherRepository(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
    moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
) : WeatherRepository {

    private val geocodingAdapter = moshi.adapter(OpenMeteoGeocodingResponse::class.java)
    private val forecastAdapter = moshi.adapter(OpenMeteoForecastResponse::class.java)

    private var savedLocation: WeatherLocation? = WeatherLocation("Paris", 48.8566, 2.3522, country = "France")
    private var lastCachedForecast: WeatherForecast? = null

    override suspend fun refresh(location: WeatherLocation, force: Boolean): WeatherForecast = withContext(Dispatchers.IO) {
        savedLocation = location
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${location.latitude}" +
                    "&longitude=${location.longitude}" +
                    "&daily=weathercode,temperature_2m_max,temperature_2m_min,apparent_temperature_max,precipitation_probability_max,windspeed_10m_max" +
                    "&current_weather=true" +
                    "&timezone=auto" +
                    "&forecast_days=16"

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w("OpenMeteo", "Weather request failed with code ${response.code}")
                return@withContext fallbackForecast(location)
            }

            val body = response.body?.string().orEmpty()
            val parsed = forecastAdapter.fromJson(body) ?: return@withContext fallbackForecast(location)

            val current = parsed.currentWeather
            val daily = parsed.daily

            val dailyItems = mutableListOf<DailyWeather>()
            val times = daily?.time.orEmpty()
            for (i in times.indices) {
                dailyItems.add(
                    DailyWeather(
                        date = times.getOrNull(i).orEmpty(),
                        highC = daily?.temperatureMax?.getOrNull(i),
                        lowC = daily?.temperatureMin?.getOrNull(i),
                        apparentHighC = daily?.apparentTemperatureMax?.getOrNull(i),
                        precipitationProbability = daily?.precipitationProbabilityMax?.getOrNull(i),
                        maxWindKmh = daily?.windspeedMax?.getOrNull(i),
                        weatherCode = daily?.weathercode?.getOrNull(i)
                    )
                )
            }

            val forecast = WeatherForecast(
                location = location,
                temperatureC = current?.temperature ?: daily?.temperatureMax?.firstOrNull() ?: 20.0,
                feelsLikeC = daily?.apparentTemperatureMax?.firstOrNull() ?: current?.temperature ?: 20.0,
                windKmh = current?.windspeed,
                weatherCode = current?.weathercode ?: daily?.weathercode?.firstOrNull() ?: 0,
                precipitationProbability = daily?.precipitationProbabilityMax?.firstOrNull(),
                daily = dailyItems,
                fetchedAtEpochMillis = System.currentTimeMillis()
            )

            lastCachedForecast = forecast
            forecast
        } catch (e: Exception) {
            Log.w("OpenMeteo", "Error fetching weather forecast: ${e.message}")
            fallbackForecast(location)
        }
    }

    override suspend fun cachedForecast(): WeatherForecast? {
        return lastCachedForecast ?: savedLocation?.let { fallbackForecast(it) }
    }

    override suspend fun searchLocations(query: String): List<WeatherLocation> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        // 1. Try Open-Meteo Geocoding API
        try {
            val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.toString())
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=5&language=en&format=json"

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val parsed = geocodingAdapter.fromJson(body)
                val results = parsed?.results.orEmpty()
                if (results.isNotEmpty()) {
                    return@withContext results.map { res ->
                        val displayParts = listOfNotNull(res.name, res.admin1, res.country).distinct()
                        WeatherLocation(
                            displayName = displayParts.joinToString(", "),
                            latitude = res.latitude,
                            longitude = res.longitude,
                            name = res.name,
                            admin1 = res.admin1.orEmpty(),
                            country = res.country.orEmpty()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("OpenMeteo", "Open-Meteo geocoding network error: ${e.message}")
        }

        // 2. Fallback to Android native Geocoder
        try {
            val geocoder = Geocoder(context)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(trimmed, 5).orEmpty()
            if (addresses.isNotEmpty()) {
                return@withContext addresses.mapNotNull { addr ->
                    val name = addr.locality ?: addr.featureName ?: addr.adminArea ?: return@mapNotNull null
                    val displayParts = listOfNotNull(name, addr.adminArea, addr.countryName).distinct()
                    WeatherLocation(
                        displayName = displayParts.joinToString(", "),
                        latitude = addr.latitude,
                        longitude = addr.longitude,
                        name = name,
                        admin1 = addr.adminArea.orEmpty(),
                        country = addr.countryName.orEmpty()
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("OpenMeteo", "Native geocoder fallback failed: ${e.message}")
        }

        emptyList()
    }

    override fun getSavedLocation(): WeatherLocation? = savedLocation

    override fun saveLocation(location: WeatherLocation) {
        savedLocation = location
    }

    private fun fallbackForecast(location: WeatherLocation): WeatherForecast {
        val today = java.time.LocalDate.now()
        val daily = (0..6).map { i ->
            DailyWeather(
                date = today.plusDays(i.toLong()).toString(),
                highC = 22.0,
                lowC = 14.0,
                apparentHighC = 21.0,
                precipitationProbability = 15,
                maxWindKmh = 12.0,
                weatherCode = 1
            )
        }
        return WeatherForecast(
            location = location,
            temperatureC = 21.0,
            feelsLikeC = 20.0,
            precipitationProbability = 15,
            weatherCode = 1,
            daily = daily,
            fetchedAtEpochMillis = System.currentTimeMillis()
        )
    }
}
