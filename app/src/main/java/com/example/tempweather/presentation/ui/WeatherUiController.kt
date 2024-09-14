package com.example.tempweather.presentation.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.tempweather.BuildConfig
import com.example.tempweather.data.network.responses.Current
import com.example.tempweather.data.network.responses.Daily
import com.example.tempweather.data.network.responses.Hourly
import com.example.tempweather.data.network.responses.LocationResponse
import com.example.tempweather.data.network.responses.WeatherResponse
import com.example.tempweather.databinding.ActivityMainBinding
import com.example.tempweather.presentation.HourlyData
import com.example.tempweather.presentation.adapter.TemperatureAdapter
import com.example.tempweather.presentation.utils.DailyWeatherUtils
import com.example.tempweather.presentation.utils.DateTimeUtils.formattedDateTime
import com.example.tempweather.presentation.utils.LocationHandler
import com.example.tempweather.presentation.utils.WeatherTheme
import com.example.tempweather.presentation.viewmodel.WeatherViewModel
import java.time.LocalDateTime

class WeatherUiController(
    private val binding: ActivityMainBinding,
    private val context: Context,
    private val window: Window,
    private val viewModel: WeatherViewModel
) {
    private val reverseGeocodeApiKey = BuildConfig.REVERSE_GEOCODE_API_KEY
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    // Main visibility handling
    fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    fun hideKeyboardAndListView() {
        // Dismiss the keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextLocation.windowToken, 0)
        // Hide the list view
        binding.listViewLocations.visibility = View.GONE
        // Clear the adapter to ensure no residual data
        binding.listViewLocations.adapter = null
    }

    private fun setMainVisibility(visibility: Int) {
        val views = listOf(
            binding.useLocationStatusButton,
            binding.textViewTemperature,
            binding.textViewWindSpeed,
            binding.textViewHumidity,
            binding.textViewFeelsLike,
            binding.imageViewWeather,
            binding.textViewCityName,
            binding.secondLayout,
            binding.recyclerViewTemperatures,
            binding.textViewTime,
            binding.thirdLayout,
            binding.dailyWeatherLayout
        )
        views.forEach { it.visibility = visibility }
    }

    private fun hideMain() {
        setMainVisibility(View.GONE)
    }

    fun showMain() {
        setMainVisibility(View.VISIBLE)
    }

    fun handleWeatherResponse(weather: WeatherResponse) {
        updateCurrentWeatherUI(weather.current)
        updateHourlyWeatherUI(weather.hourly, weather.current.time)
        updateDailyWeatherUI(weather.daily)

        showLoading(false)
        hideKeyboardAndListView()
    }

    private fun updateCurrentWeatherUI(currentWeather: Current) {
        binding.apply {
            textViewTemperature.text = "${currentWeather.temperature} °C"
            fahTextViewTemperature.text = "${convertCelciusToFahrenheit(currentWeather.temperature.toFloat())} °F"
            textViewHumidity.text = "${currentWeather.humidity} %"
            textViewWindSpeed.text = "${currentWeather.windSpeed} km/h"
            textViewFeelsLike.text = "${currentWeather.apparentTemperature} °C"

            val weatherIconResId = WeatherTheme.getWeatherIcon(currentWeather.weatherCode, currentWeather.isDay)
            imageViewWeather.setImageResource(weatherIconResId)
            textViewTime.text = formattedDateTime(currentWeather.time)

            WeatherTheme.changeTheme(
                currentWeather.weatherCode,
                currentWeather.isDay,
                context,
                window,
                mainLayout
            )
        }
    }

    private fun updateHourlyWeatherUI(hourlyWeather: Hourly, currentTime: String) {
        val currentDateTime = LocalDateTime.parse(currentTime)
        // Combine the hourly weather data into a list of HourlyData objects
        val hourlyData = hourlyWeather.time.indices
            .map { index ->
                HourlyData(
                    time = hourlyWeather.time[index],
                    temperature = hourlyWeather.temperature[index],
                    weatherCode = hourlyWeather.weatherCode[index],
                    isDay = hourlyWeather.isDay[index]
                )
            }
            // Filter to include only data after the current time
            .filter { data -> LocalDateTime.parse(data.time).isAfter(currentDateTime) }
            // Take the next 24 hours of data
            .take(24)

        val adapter = binding.recyclerViewTemperatures.adapter as? TemperatureAdapter
        if (adapter == null) {
            // Create a new adapter if it doesn't exist
            binding.recyclerViewTemperatures.adapter = TemperatureAdapter(hourlyData)
        } else {
            // Update the existing adapter's data
            adapter.updateData(hourlyData)
        }
        binding.recyclerViewTemperaturesCV.visibility = View.VISIBLE
    }

    private fun updateDailyWeatherUI(dailyWeather: Daily) {
        binding.apply {
            // Update Sunrise and Sunset
            sunRiseTextView.text = dailyWeather.sunrise[0].split('T')[1]
            sunSetTextView.text = dailyWeather.sunset[0].split('T')[1]
            // Update Daily Weather
            dailyWeatherLayout.removeAllViews()
            for (i in dailyWeather.time.indices) {
                DailyWeatherUtils.addWeatherData(
                    context,
                    dailyWeatherLayout,
                    dailyWeather.time[i],
                    dailyWeather.precipitationProbabilityMax[i],
                    dailyWeather.temperatureMax[i],
                    dailyWeather.temperatureMin[i]
                )
            }
            dailyWeatherLayoutCV.visibility = View.VISIBLE
        }
    }

    fun handleWeatherFailure() {
        binding.apply {
            textViewTemperature.text = "--°C"
            textViewHumidity.text = "--%"
            textViewWindSpeed.text = "--km/h"
            textViewFeelsLike.text = "--°C"
            imageViewWeather.setImageResource(WeatherTheme.getWeatherIcon(weatherCode = -1, isDay = -1))
            textViewTime.text = ""
            recyclerViewTemperaturesCV.visibility = View.GONE
            dailyWeatherLayoutCV.visibility = View.GONE
            sunRiseTextView.text = "-"
            sunSetTextView.text = "-"
        }
        showLoading(false)
        hideKeyboardAndListView()
    }

    fun updateLocationList(locations: List<LocationResponse>) {
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_1,
            locations.map { location -> location.displayName }
        )
        binding.listViewLocations.adapter = adapter

        handleLocationItemClick(locations)
        hideMain()
        binding.listViewLocations.visibility = View.VISIBLE
    }

    // Call viewModel
    private fun handleLocationItemClick(locations: List<LocationResponse>) {
        binding.listViewLocations.setOnItemClickListener { _, _, position, _ ->
            val selectedLocation = locations[position]

            // Fetch weather and city name based on the selected location's latitude and longitude
            viewModel.fetchWeather(selectedLocation.latitude.toDouble(), selectedLocation.longitude.toDouble())
            // Hide keyboard, clear input, and show main content
            hideKeyboardAndListView()
            binding.editTextLocation.setText("")
            showMain()
        }
    }

    fun checkLocationAndFetchData() {
        showLoading(isLoading = true)
        // Check if location permissions are granted
        if (LocationHandler.isLocationPermissionGranted(context)) {
            // Check if location services are enabled
            if (LocationHandler.isLocationEnabled(context)) {
                // Fetch the current location
                LocationHandler.getLocation(context) { location ->
                    location?.let {
                        viewModel.fetchWeather(it.latitude, it.longitude)
                    } ?: run {
                        Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show()
                        showLoading(isLoading = false)
                    }
                }
            } else {
                Toast.makeText(context, "Location disabled", Toast.LENGTH_SHORT).show()
                showLoading(isLoading = false)
            }
        } else {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            showLoading(isLoading = false)
            LocationHandler.requestLocationPermission(context as Activity, LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    // Converts to fahrenheit
    private fun convertCelciusToFahrenheit(celsius: Float): Float {
        return ((celsius * 9) / 5) + 32
    }
}