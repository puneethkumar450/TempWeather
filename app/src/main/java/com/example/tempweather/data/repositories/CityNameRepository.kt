package com.example.tempweather.data.repositories

import com.example.tempweather.data.network.services.AddressNameService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface CityNameRepository {
    suspend fun fetchCityName(latitude: Double, longitude: Double, reverseGeocodeApiKey: String): String?
}

class CityNameRepositoryImpl : CityNameRepository {

    private val addressNameService: AddressNameService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://geocode.maps.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        addressNameService = retrofit.create(AddressNameService::class.java)
    }

    override suspend fun fetchCityName(latitude: Double, longitude: Double, reverseGeocodeApiKey: String): String? {
        return try {
            val response = addressNameService.getAddressName(latitude, longitude, reverseGeocodeApiKey)
            val address = response.address
            when {
                address.village != null && address.country != null -> "${address.village},\n${address.country}"
                address.village != null -> address.village
                address.town != null && address.country != null -> "${address.town},\n${address.country}"
                address.town != null -> address.town
                address.city != null -> address.city
                address.region != null -> address.region
                address.stateDistrict != null -> address.stateDistrict
                address.hamlet != null -> address.hamlet
                address.suburb != null -> address.suburb
                address.country != null -> address.country
                else -> ""
            }
        } catch (e: Exception) {
            null
        }
    }
}