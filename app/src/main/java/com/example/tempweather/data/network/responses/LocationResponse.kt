package com.example.tempweather.data.network.responses

import com.google.gson.annotations.SerializedName

data class LocationResponse(
    @SerializedName("place_id") val placeId: String,
    @SerializedName("lat") val latitude: String,
    @SerializedName("lon") val longitude: String,
    @SerializedName("display_name") val displayName: String
)
