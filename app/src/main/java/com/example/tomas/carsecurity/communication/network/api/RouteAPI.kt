package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface RouteAPI {

    @FormUrlEncoded
    @POST(Mapping.ROUTE_URL)
    fun createRoute(@Field("car_id") carId: Long): Call<Any>



}