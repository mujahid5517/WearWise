package com.example.data.repository

import android.content.Context
import com.example.domain.model.DailyWeather
import com.example.domain.model.HourlyWeather
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
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class OpenMeteoWeatherRepository(
    context: Context
) : WeatherRepository {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val forecastAdapter = moshi.adapter(WeatherForecast::class.java)
    private val locationAdapter = moshi.adapter(WeatherLocation::class.java)
    private val responseAdapter = moshi.adapter(OpenMeteoResponse::class.java)
    private val locationsAdapter = moshi.adapter(GeocodingResponse::class.java)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun refresh(location: WeatherLocation, force: Boolean): WeatherForecast =
        withContext(Dispatchers.IO) {
            require(location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0) {
                "Choose a valid weather location."
            }
            val cached = readCachedForecast()
            val sameLocation = cached?.location?.let {
                kotlin.math.abs(it.latitude - location.latitude) < 0.01 &&
                    kotlin.math.abs(it.longitude - location.longitude) < 0.01
            } == true
            if (!force && sameLocation &&
                System.currentTimeMillis() - cached!!.fetchedAtEpochMillis < CACHE_FRESH_MS
            ) {
                return@withContext cached.copy(isCached = true)
            }

            val url = FORECAST_URL.toHttpUrl().newBuilder()
                .addQueryParameter("latitude", location.latitude.toString())
                .addQueryParameter("longitude", location.longitude.toString())
                .addQueryParameter("current", CURRENT_VARIABLES)
                .addQueryParameter("hourly", HOURLY_VARIABLES)
                .addQueryParameter("daily", DAILY_VARIABLES)
                .addQueryParameter("forecast_days", "7")
                .addQueryParameter("temperature_unit", "celsius")
                .addQueryParameter("wind_speed_unit", "kmh")
                .addQueryParameter("precipitation_unit", "mm")
                .addQueryParameter("timezone", "auto")
                .build()
            val response = responseAdapter.fromJson(execute(url.toString()))
                ?: throw IOException("Open-Meteo returned an empty weather response.")
            val current = response.current
                ?: throw IOException("Open-Meteo response did not contain current conditions.")
            val hourly = response.hourly
            val daily = response.daily
                ?: throw IOException("Open-Meteo response did not contain a daily forecast.")
            if (daily.time.isEmpty()) {
                throw IOException("Open-Meteo returned an incomplete daily forecast.")
            }

            val hourlyEntries = hourly?.let { block ->
                block.time.mapIndexed { index, time ->
                    HourlyWeather(
                        time = time,
                        weatherCode = block.weatherCode.at(index),
                        temperatureC = block.temperature.at(index),
                        feelsLikeC = block.apparentTemperature.at(index),
                        precipitationProbability = block.precipitationProbability.at(index)?.toInt(),
                        precipitationMm = block.precipitation.at(index),
                        humidityPercent = block.humidity.at(index)?.toInt(),
                        windKmh = block.windSpeed.at(index),
                        cloudCoverPercent = block.cloudCover.at(index)?.toInt()
                    )
                }
            }.orEmpty()
            val dailyEntries = daily.time.indices.map { index ->
                DailyWeather(
                    date = daily.time[index],
                    highC = daily.temperatureMax.at(index),
                    lowC = daily.temperatureMin.at(index),
                    apparentHighC = daily.apparentTemperatureMax.at(index),
                    precipitationProbability = daily.precipitationProbabilityMax.at(index)?.toInt(),
                    precipitationMm = daily.precipitationSum.at(index),
                    maxWindKmh = daily.windSpeedMax.at(index),
                    weatherCode = daily.weatherCode.at(index)
                )
            }
            val updatedAt = System.currentTimeMillis()
            val result = WeatherForecast(
                location = location,
                temperatureC = current.temperature,
                feelsLikeC = current.apparentTemperature,
                precipitationProbability = hourlyEntries.nearestToNow(response.timezone)?.precipitationProbability,
                precipitationMm = current.precipitation,
                windKmh = current.windSpeed,
                weatherCode = current.weatherCode,
                humidityPercent = current.humidity?.toInt(),
                fetchedAtEpochMillis = updatedAt,
                daily = dailyEntries,
                hourly = hourlyEntries,
                cloudCoverPercent = current.cloudCover?.toInt(),
                timezone = response.timezone?.takeIf(String::isNotBlank) ?: "UTC",
                isCached = false
            )
            preferences.edit()
                .putString(KEY_FORECAST, forecastAdapter.toJson(result))
                .putString(KEY_LOCATION, locationAdapter.toJson(result.location))
                .apply()
            result
        }

    override suspend fun cachedForecast(): WeatherForecast? = withContext(Dispatchers.IO) {
        readCachedForecast()?.copy(isCached = true)
    }

    override suspend fun searchLocations(query: String): List<WeatherLocation> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        require(trimmed.length >= 2) { "Enter at least two characters to search for a city." }
        val url = GEOCODING_URL.toHttpUrl().newBuilder()
            .addQueryParameter("name", trimmed)
            .addQueryParameter("count", "8")
            .addQueryParameter("language", "en")
            .addQueryParameter("format", "json")
            .build()
        val response = locationsAdapter.fromJson(execute(url.toString()))
        response?.results.orEmpty().map { result ->
            val displayName = listOfNotNull(
                result.name,
                result.admin1?.takeIf { it != result.name },
                result.country
            ).filter(String::isNotBlank).joinToString(", ")
            WeatherLocation(
                displayName = displayName,
                latitude = result.latitude,
                longitude = result.longitude,
                name = result.name,
                admin1 = result.admin1.orEmpty(),
                country = result.country.orEmpty()
            )
        }
    }

    override fun getSavedLocation(): WeatherLocation? =
        preferences.getString(KEY_LOCATION, null)?.let { json ->
            runCatching { locationAdapter.fromJson(json) }.getOrNull()
        }

    override fun saveLocation(location: WeatherLocation) {
        preferences.edit().putString(KEY_LOCATION, locationAdapter.toJson(location)).apply()
    }

    private fun readCachedForecast(): WeatherForecast? =
        preferences.getString(KEY_FORECAST, null)?.let { json ->
            runCatching { forecastAdapter.fromJson(json) }.getOrNull()
        }

    private fun execute(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "WearWise-Android")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val detail = response.body?.string()?.take(240).orEmpty()
                val explanation = when (response.code) {
                    401, 403 -> "Open-Meteo denied the request (HTTP ${response.code})."
                    429 -> "Open-Meteo rate limit reached. Wait before refreshing."
                    in 500..599 -> "Open-Meteo is temporarily unavailable (HTTP ${response.code})."
                    else -> "Open-Meteo request failed (HTTP ${response.code})."
                }
                throw IOException(listOf(explanation, detail).filter(String::isNotBlank).joinToString(" "))
            }
            return response.body?.string()
                ?: throw IOException("Open-Meteo returned an empty response body.")
        }
    }

    private fun <T> List<T>?.at(index: Int): T? = this?.getOrNull(index)

    private fun List<HourlyWeather>.nearestToNow(timezone: String?): HourlyWeather? {
        val zone = runCatching { ZoneId.of(timezone ?: "UTC") }.getOrDefault(ZoneId.of("UTC"))
        val now = LocalDateTime.ofInstant(Instant.now(), zone)
        return minByOrNull { hour ->
            val hourTime = runCatching { LocalDateTime.parse(hour.time) }.getOrNull()
            hourTime?.let { kotlin.math.abs(java.time.Duration.between(now, it).toMinutes()) }
                ?: Long.MAX_VALUE
        }
    }

    @JsonClass(generateAdapter = true)
    internal data class OpenMeteoResponse(
        val timezone: String? = null,
        val current: CurrentBlock? = null,
        val hourly: HourlyBlock? = null,
        val daily: DailyBlock? = null
    )

    @JsonClass(generateAdapter = true)
    internal data class CurrentBlock(
        @param:Json(name = "temperature_2m") val temperature: Double? = null,
        @param:Json(name = "relative_humidity_2m") val humidity: Double? = null,
        @param:Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
        val precipitation: Double? = null,
        @param:Json(name = "weather_code") val weatherCode: Int? = null,
        @param:Json(name = "cloud_cover") val cloudCover: Double? = null,
        @param:Json(name = "wind_speed_10m") val windSpeed: Double? = null
    )

    @JsonClass(generateAdapter = true)
    internal data class HourlyBlock(
        val time: List<String> = emptyList(),
        @param:Json(name = "temperature_2m") val temperature: List<Double?>? = null,
        @param:Json(name = "apparent_temperature") val apparentTemperature: List<Double?>? = null,
        @param:Json(name = "precipitation_probability") val precipitationProbability: List<Double?>? = null,
        val precipitation: List<Double?>? = null,
        @param:Json(name = "relative_humidity_2m") val humidity: List<Double?>? = null,
        @param:Json(name = "wind_speed_10m") val windSpeed: List<Double?>? = null,
        @param:Json(name = "weather_code") val weatherCode: List<Int?>? = null,
        @param:Json(name = "cloud_cover") val cloudCover: List<Double?>? = null
    )

    @JsonClass(generateAdapter = true)
    internal data class DailyBlock(
        val time: List<String> = emptyList(),
        @param:Json(name = "temperature_2m_max") val temperatureMax: List<Double?>? = null,
        @param:Json(name = "temperature_2m_min") val temperatureMin: List<Double?>? = null,
        @param:Json(name = "apparent_temperature_max") val apparentTemperatureMax: List<Double?>? = null,
        @param:Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Double?>? = null,
        @param:Json(name = "precipitation_sum") val precipitationSum: List<Double?>? = null,
        @param:Json(name = "wind_speed_10m_max") val windSpeedMax: List<Double?>? = null,
        @param:Json(name = "weather_code") val weatherCode: List<Int?>? = null
    )

    @JsonClass(generateAdapter = true)
    internal data class GeocodingResponse(val results: List<GeocodingResult>? = null)

    @JsonClass(generateAdapter = true)
    internal data class GeocodingResult(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val admin1: String? = null,
        val country: String? = null
    )

    companion object {
        private const val PREFERENCES = "wearwise_open_meteo"
        private const val KEY_FORECAST = "latest_forecast"
        private const val KEY_LOCATION = "weather_location"
        private const val CACHE_FRESH_MS = 5 * 60 * 1000L
        private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
        private const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
        private const val CURRENT_VARIABLES =
            "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,cloud_cover,wind_speed_10m"
        private const val HOURLY_VARIABLES =
            "temperature_2m,apparent_temperature,precipitation_probability,precipitation,relative_humidity_2m,wind_speed_10m,weather_code,cloud_cover"
        private const val DAILY_VARIABLES =
            "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,precipitation_probability_max,precipitation_sum,wind_speed_10m_max"
    }
}
