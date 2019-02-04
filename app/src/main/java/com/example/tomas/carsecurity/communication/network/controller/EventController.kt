package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.EventAPI
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EventController(serverUrl: String) {

    private val tag = "EventController"
    private val eventAPI: EventAPI

    init {
        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        eventAPI = retrofit.create(EventAPI::class.java)
    }

    fun createEvent(event: String): Response<Void> {
        val requestBody = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), event)
        val method = eventAPI.createEvent(requestBody)

        Log.d(tag, "Sending message to create event endpoint. URL: ${method.request().url()}")
        return try {
            method.execute()
        } catch (e: Exception) {
            Log.d(tag, "Can not send request. Exception: ${e.printStackTrace()}")
            Response.error(418, ResponseBody.create(null, ""))
        }
    }
}