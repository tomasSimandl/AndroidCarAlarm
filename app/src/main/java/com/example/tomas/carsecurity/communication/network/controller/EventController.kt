package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.EventAPI
import com.example.tomas.carsecurity.communication.network.dto.EventCreate
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EventController(serverUrl: String) {

    private val tag = "EventController"
    private val eventAPI: EventAPI

    init {
        Log.d(tag, "Initializing EventController")
        val retrofit = Retrofit.Builder()
                    .baseUrl(serverUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

        eventAPI = retrofit.create(EventAPI::class.java)

    }

    fun createEvent(event: EventCreate): Boolean{
        val method = eventAPI.createEvent(event)

        Log.d(tag, "Sending message to create event endpoint. URL: ${method.request().url()}")
        return method.execute().isSuccessful
    }
}