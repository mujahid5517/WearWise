package com.example.data.repository

import android.content.Context
import com.example.domain.model.WeatherForecast
import com.example.domain.model.WeatherLocation
import com.example.domain.repository.WeatherRepository

class OpenMeteoWeatherRepository(
    private val context: Context
) : WeatherRepository {
    override suspend fun refresh(location: WeatherLocation, force: Boolean): WeatherForecast {
        return WeatherForecast()
    }

    override suspend fun cachedForecast(): WeatherForecast? = WeatherForecast()

    override suspend fun searchLocations(query: String): List<WeatherLocation> {
        return listOf(WeatherLocation(name = query))
    }

    override fun getSavedLocation(): WeatherLocation? = WeatherLocation()

    override fun saveLocation(location: WeatherLocation) {}
}
