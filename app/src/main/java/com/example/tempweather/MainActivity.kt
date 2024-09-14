package com.example.tempweather

import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.tempweather.data.db.WeatherDatabase
import com.example.tempweather.databinding.ActivityMainBinding
import com.example.tempweather.data.repositories.*
import com.example.tempweather.presentation.adapter.TemperatureAdapter
import com.example.tempweather.presentation.ui.WeatherUiController
import com.example.tempweather.presentation.utils.LocationHandler.requestLocationPermission
import com.example.tempweather.presentation.viewmodel.LocationState
import com.example.tempweather.presentation.viewmodel.WeatherState
import com.example.tempweather.presentation.viewmodel.WeatherViewModel
import com.example.tempweather.presentation.viewmodel.WeatherViewModelFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var locationRepository: LocationRepository
    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModelFactory(
            weatherRepository,
            CurrentWeatherRepositoryImpl(WeatherDatabase.getDatabase(this).currentWeatherDao()),
            DailyWeatherRepositoryImpl(WeatherDatabase.getDatabase(this).dailyWeatherDao()),
            HourlyWeatherRepositoryImpl(WeatherDatabase.getDatabase(this).hourlyWeatherDao()),
            locationRepository
        )
    }
    private lateinit var uiController: WeatherUiController
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val locationIQApiKey = BuildConfig.LOCATION_IQ_API_KEY
    private var isKeyboardVisible = false
    private var backPressCount = 0
    private val handler = Handler(Looper.getMainLooper())

    private fun init()
    {
        weatherRepository = WeatherRepositoryImpl()
        locationRepository = LocationRepositoryImpl()
        uiController = WeatherUiController(binding, this, this.window, viewModel)
        setupRecyclerView()
        setupTheme()
        setupViewModelObserver()
        setupListeners()
        // Trigger onRequestPermissionsResult
        requestLocationPermission(this, LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    override fun onBackPressed() {
        if (isKeyboardVisible || binding.listViewLocations.visibility == View.VISIBLE) {
            // Dismiss the keyboard and list view
            uiController.hideKeyboardAndListView()
        } else {
            if (backPressCount == 0) {
                backPressCount++
                // Inform user to press again to exit
                Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show()
                // Reset the count after 2 seconds
                handler.postDelayed({ backPressCount = 0 }, 2000)
            } else {
                super.onBackPressed()
            }
        }
        binding.editTextLocation.clearFocus()
        uiController.showMain()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with location fetch from API
                uiController.checkLocationAndFetchData()
            } else {
                Log.e("WeatherApp", "Location permission denied.")
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewTemperatures.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // Initialize recyclerViewTemperatures with an empty list
        binding.recyclerViewTemperatures.adapter = TemperatureAdapter(listOf())
    }

    //ViewModel Observer
    private fun setupViewModelObserver() {
        viewModel.weatherState.observe(this) { state ->
            when (state) {
                is WeatherState.Loading -> uiController.showLoading(true)
                is WeatherState.Success -> uiController.handleWeatherResponse(state.weatherResponse)
                is WeatherState.Error -> uiController.handleWeatherFailure()
            }
        }
        viewModel.latLongState.observe(this) { state ->
            binding.textViewCityName.text = state
        }
        viewModel.locationState.observe(this) { state ->
            when (state) {
                is LocationState.Success -> uiController.updateLocationList(state.locationResponse)
                is LocationState.Error -> Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Event Listeners
    private fun setupListeners() {
        // Listener to detect keyboard visibility
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            isKeyboardVisible = keypadHeight > screenHeight * 0.15 // 15% of the screen height to consider it keyboard
        }

        // Use Location button listener
        binding.useLocationStatusButton.setOnClickListener {
            uiController.checkLocationAndFetchData()
            binding.editTextLocation.clearFocus()
        }

        // User search location listener
        binding.editTextLocation.addTextChangedListener(object : TextWatcher {
            private var debounceJob: Job? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                debounceJob?.cancel()
                debounceJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(1000) // debounce delay
                    val query = s.toString()
                    if (query.isNotEmpty()) {
                        viewModel.getAutocomplete(query,locationIQApiKey)
                    } else {
                        binding.editTextLocation.clearFocus()
                        uiController.hideKeyboardAndListView()
                        uiController.showMain()
                    }
                }
            }
        })
    }

    private fun setupTheme() {
        // Set the status and navigation bar colors
        window.statusBarColor = ContextCompat.getColor(this, R.color.Clear_Day_Blue)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.Clear_Day_Blue)
    }
}