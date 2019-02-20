package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CarAPI {

    @GET(Mapping.CAR_URL)
    fun getCars(): Call<Any>

    @POST(Mapping.CAR_URL)
    fun createCar(@Body car: RequestBody): Call<Any>
}