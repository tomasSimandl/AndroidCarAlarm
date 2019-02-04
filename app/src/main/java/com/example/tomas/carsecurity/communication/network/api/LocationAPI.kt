package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import com.example.tomas.carsecurity.communication.network.dto.EventCreate
import com.example.tomas.carsecurity.storage.entity.Location
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface LocationAPI {

    @POST(Mapping.LOCATION_URL)
    fun createLocations(@Body location: List<Location>): Call<Void>
}