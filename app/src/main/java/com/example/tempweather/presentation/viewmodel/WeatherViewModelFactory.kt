package com.example.tempweather.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tempweather.data.repositories.*

class WeatherViewModelFactory(
    private val weatherApiRepository: WeatherRepository,
    private val currentWeatherRepository: CurrentWeatherRepository,
    private val dailyWeatherRepository: DailyWeatherRepository,
    private val hourlyWeatherRepository: HourlyWeatherRepository,
    private val locationRepository: LocationRepository

) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(
                weatherApiRepository,
                currentWeatherRepository,
                dailyWeatherRepository,
                hourlyWeatherRepository,
                locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}