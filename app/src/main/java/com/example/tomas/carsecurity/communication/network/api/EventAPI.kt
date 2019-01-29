package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import com.example.tomas.carsecurity.communication.network.dto.EventCreate
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface EventAPI {

    @POST(Mapping.EVENT_URL)
    fun createEvent(@Body event: EventCreate): Call<Any>



}