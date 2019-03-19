package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit API for outgoing network requests related to car object.
 */
interface CarAPI {

    /**
     * Prepare request to server which return a list of users cars.
     */
    @GET(Mapping.CAR_URL)
    fun getCars(): Call<Any>

    /**
     * Prepare request to server to create new car which is defined by input object.
     */
    @POST(Mapping.CAR_URL)
    fun createCar(@Body car: RequestBody): Call<Any>
}