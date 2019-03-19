package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import com.example.tomas.carsecurity.communication.network.dto.StatusCreate
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API for outgoing network requests related Status object.
 */
interface StatusAPI {

    /**
     * Prepare request to server to create a status which is defined by input object.
     */
    @POST(Mapping.STATUS_URL)
    fun createStatus(@Body status: StatusCreate): Call<Void>
}