package com.example.tempweather.data.repositories

import android.util.Log
import com.example.tempweather.data.db.dao.HourlyWeatherDao
import com.example.tempweather.data.db.entities.HourlyWeatherEntity
import com.example.tempweather.data.network.responses.Hourly

interface HourlyWeatherRepository {
    suspend fun insertHourlyWeather(hourly: Hourly, currentTime: String)
    suspend fun getHourlyWeather(): List<HourlyWeatherEntity>
    suspend fun clearHourlyWeather()
    suspend fun logHourlyWeather()
}

class HourlyWeatherRepositoryImpl(private val hourlyWeatherDao: HourlyWeatherDao) : HourlyWeatherRepository {

    override suspend fun insertHourlyWeather(hourly: Hourly, currentTime: String) {
        try {
            // First Clear the old hourly weather data
            clearHourlyWeather()
            // Prepare and insert new hourly weather data for the next 24 hours
            val entities = hourly.time.indices.map { i ->
                HourlyWeatherEntity(
                    time = hourly.time[i],
                    temperature = hourly.temperature[i],
                    weatherCode = hourly.weatherCode[i],
                    isDay = hourly.isDay[i]
                )
            }.take(24)
            hourlyWeatherDao.insertHourlyWeather(entities)
            Log.d("HourlyWeatherRepo", "insertHourlyWeather: Insertion successful")
        } catch (e: Exception) {
            Log.e("HourlyWeatherRepo", "insertHourlyWeather: Insertion failed", e)
        }
    }

    override suspend fun getHourlyWeather(): List<HourlyWeatherEntity> {
        return try {
            hourlyWeatherDao.getHourlyWeather()
        } catch (e: Exception) {
            Log.e("HourlyWeatherRepo", "getHourlyWeather: Retrieval failed", e)
            emptyList()
        }
    }

    override suspend fun clearHourlyWeather() {
        try {
            hourlyWeatherDao.deleteHourlyWeather()
            Log.d("HourlyWeatherRepo", "clearHourlyWeather: Clear successful")
        } catch (e: Exception) {
            Log.e("HourlyWeatherRepo", "clearHourlyWeather: Clear failed", e)
        }
    }

    override suspend fun logHourlyWeather() {
        try {
            val hourlyWeatherList = getHourlyWeather()
            if (hourlyWeatherList.isNotEmpty()) {
                hourlyWeatherList.forEach { hourlyWeather ->
                    Log.d("HourlyWeatherLog", "hourlyWeather Data : $hourlyWeather")
                    Log.d("HourlyWeatherLog", "----------------------")
                }
            } else {
                Log.d("HourlyWeatherLog", "No hourly weather data available.")
            }
        } catch (e: Exception) {
            Log.e("HourlyWeatherLog", "Error fetching hourly weather data", e)
        }
    }
}