package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import com.example.tomas.carsecurity.storage.entity.Location
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API for outgoing network requests related to Location object.
 */
interface LocationAPI {

    /**
     * Prepare request to server to create new locations specified with given list of locations.
     */
    @POST(Mapping.LOCATION_URL)
    fun createLocations(@Body location: List<Location>): Call<Void>
}