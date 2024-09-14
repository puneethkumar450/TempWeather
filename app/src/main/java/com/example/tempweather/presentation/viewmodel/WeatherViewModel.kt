package com.example.tempweather.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tempweather.data.network.responses.*
import com.example.tempweather.data.repositories.*
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val weatherApiRepository: WeatherRepository,
    private val currentWeatherRepository: CurrentWeatherRepository,
    private val dailyWeatherRepository: DailyWeatherRepository,
    private val hourlyWeatherRepository: HourlyWeatherRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _weatherState = MutableLiveData<WeatherState>()
    val weatherState: LiveData<WeatherState> = _weatherState

    private val _latLongState = MutableLiveData<String>()
    val latLongState: LiveData<String> = _latLongState

    private val _locationState = MutableLiveData<LocationState>()
    val locationState: LiveData<LocationState> = _locationState

    fun fetchWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            val weatherResponse = weatherApiRepository.fetchWeatherData(latitude, longitude)
            weatherResponse?.let {
                _weatherState.value = WeatherState.Success(it)
                saveWeatherData(it)
            } ?: run {
                _weatherState.value = WeatherState.Error("Failed to fetch weather data")
            }
            _latLongState.value =  "LatLong : $latitude $longitude"
        }
    }

    private fun saveWeatherData(weather: WeatherResponse) {
        viewModelScope.launch {
            currentWeatherRepository.insertCurrentWeather(weather.current)
            dailyWeatherRepository.insertDailyWeather(weather.daily)
            hourlyWeatherRepository.insertHourlyWeather(weather.hourly, weather.current.time)
        }
    }

    fun fetchCityName(latitude: Double, longitude: Double, reverseGeocodeApiKey: String) {

    }

    fun getAutocomplete(query: String, apiKey: String) {
        viewModelScope.launch {
            val locations = locationRepository.getAutocomplete(query, apiKey)
            locations?.let {
                _locationState.value = LocationState.Success(locations)
            } ?: run {
                _locationState.value = LocationState.Error("Failed to load location data")
            }
        }
    }

}

sealed class WeatherState {
    data object Loading : WeatherState()
    data class Success(val weatherResponse: WeatherResponse) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

sealed class CityNameState {
    data class Success(val cityName: String) : CityNameState()
    data class Error(val message: String) : CityNameState()
}

sealed class LocationState {
    data class Success(val locationResponse: List<LocationResponse>) : LocationState()
    data class Error(val message: String) : LocationState()
}