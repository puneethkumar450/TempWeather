package com.example.tempweather.data.repositories

import android.util.Log
import com.example.tempweather.data.db.dao.DailyWeatherDao
import com.example.tempweather.data.db.entities.DailyWeatherEntity
import com.example.tempweather.data.network.responses.Daily

interface DailyWeatherRepository {
    suspend fun insertDailyWeather(daily: Daily)
    suspend fun getDailyWeather(): List<DailyWeatherEntity>
    suspend fun clearDailyWeather()
    suspend fun logDailyWeather()
}

class DailyWeatherRepositoryImpl(private val dailyWeatherDao: DailyWeatherDao) : DailyWeatherRepository {

    override suspend fun insertDailyWeather(daily: Daily) {
        try {
            // Clear the old daily weather data
            clearDailyWeather()
            // Prepare and insert new daily weather data
            val entities = daily.time.indices.map { i ->
                DailyWeatherEntity(
                    date = daily.time[i],
                    weatherCode = daily.weatherCode[i],
                    maxTemperature = daily.temperatureMax[i],
                    minTemperature = daily.temperatureMin[i],
                    sunrise = daily.sunrise[i],
                    sunset = daily.sunset[i],
                    precipitationProbability = daily.precipitationProbabilityMax[i]
                )
            }
            dailyWeatherDao.insertDailyWeather(entities)
            Log.d("DailyWeatherRepo", "insertDailyWeather: Insertion successful")
        } catch (e: Exception) {
            Log.e("DailyWeatherRepo", "insertDailyWeather: Insertion failed", e)
        }
    }

    override suspend fun getDailyWeather(): List<DailyWeatherEntity> {
        return try {
            dailyWeatherDao.getDailyWeather()
        } catch (e: Exception) {
            Log.e("DailyWeatherRepo", "getDailyWeather: Retrieval failed", e)
            emptyList()
        }
    }

    override suspend fun clearDailyWeather() {
        try {
            dailyWeatherDao.deleteDailyWeather()
            Log.d("DailyWeatherRepo", "clearDailyWeather: Clear successful")
        } catch (e: Exception) {
            Log.e("DailyWeatherRepo", "clearDailyWeather: Clear failed", e)
        }
    }

    override suspend fun logDailyWeather() {
        try {
            val dailyWeatherList = getDailyWeather()
            if (dailyWeatherList.isNotEmpty()) {
                dailyWeatherList.forEach { dailyWeather ->
                    Log.d("DailyWeatherLog", "Data: $dailyWeather")
                    Log.d("DailyWeatherLog", "----------------------")
                }
            } else {
                Log.d("DailyWeatherLog", "No daily weather data available.")
            }
        } catch (e: Exception) {
            Log.e("DailyWeatherLog", "Error fetching daily weather data", e)
        }
    }
}