package com.example.tempweather.data.repositories

import com.example.tempweather.data.network.responses.LocationResponse
import com.example.tempweather.data.network.services.LocationIQService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface LocationRepository {
    suspend fun getAutocomplete(query: String, apiKey: String): List<LocationResponse>?
}

class LocationRepositoryImpl : LocationRepository {

    private val locationIQService: LocationIQService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://us1.locationiq.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        locationIQService = retrofit.create(LocationIQService::class.java)
    }

    override suspend fun getAutocomplete(query: String, apiKey: String): List<LocationResponse>? {
        return try {
            locationIQService.getAutocomplete(apiKey, query)
        } catch (e: Exception) {
            null
        }
    }
}