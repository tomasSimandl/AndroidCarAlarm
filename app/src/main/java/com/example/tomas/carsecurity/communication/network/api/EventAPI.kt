package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API for outgoing network requests related to Event object.
 */
interface EventAPI {

    /**
     * Prepare request to server which create an event defined with input object.
     */
    @POST(Mapping.EVENT_URL)
    fun createEvent(@Body event: RequestBody): Call<Void>
}