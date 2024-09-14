package com.example.tempweather.data.repositories

import android.util.Log
import com.example.tempweather.data.db.dao.CurrentWeatherDao
import com.example.tempweather.data.db.entities.CurrentWeatherEntity
import com.example.tempweather.data.network.responses.Current

interface CurrentWeatherRepository {
    suspend fun insertCurrentWeather(current: Current)
    suspend fun getCurrentWeather(): CurrentWeatherEntity?
    suspend fun clearCurrentWeather()
    suspend fun logCurrentWeather()
}

class CurrentWeatherRepositoryImpl(private val currentWeatherDao: CurrentWeatherDao) : CurrentWeatherRepository {

    override suspend fun insertCurrentWeather(current: Current) {
        try {
            // Clear the old current weather data
            clearCurrentWeather()

            // Insert new current weather data
            val entity = CurrentWeatherEntity(
                time = current.time,
                temperature = current.temperature,
                humidity = current.humidity,
                apparentTemperature = current.apparentTemperature,
                isDay = current.isDay,
                weatherCode = current.weatherCode,
                windSpeed = current.windSpeed
            )
            currentWeatherDao.insertCurrentWeather(entity)
            Log.d("CurrentWeatherRepo", "insertCurrentWeather: Insertion successful")
        } catch (e: Exception) {
            Log.e("CurrentWeatherRepo", "insertCurrentWeather: Insertion failed", e)
        }
    }

    override suspend fun getCurrentWeather(): CurrentWeatherEntity? {
        return try {
            currentWeatherDao.getCurrentWeather()
        } catch (e: Exception) {
            Log.e("CurrentWeatherRepo", "getCurrentWeather: Retrieval failed", e)
            null
        }
    }

    override suspend fun clearCurrentWeather() {
        try {
            currentWeatherDao.deleteRecords()
            Log.d("CurrentWeatherRepo", "clearCurrentWeather: Clear successful")
        } catch (e: Exception) {
            Log.e("CurrentWeatherRepo", "clearCurrentWeather: Clear failed", e)
        }
    }

    override suspend fun logCurrentWeather() {
        try {
            val currentWeather = getCurrentWeather()
            if (currentWeather != null) {
                Log.d("CurrentWeatherLog", "CurrentWeather Data: ${currentWeather.time}")
                Log.d("CurrentWeatherLog", "----------------------")
            } else {
                Log.d("CurrentWeatherLog", "No current weather data available.")
            }
        } catch (e: Exception) {
            Log.e("CurrentWeatherLog", "Error fetching current weather data", e)
        }
    }
}