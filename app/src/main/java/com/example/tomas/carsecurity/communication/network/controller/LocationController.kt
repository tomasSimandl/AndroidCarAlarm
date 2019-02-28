package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.LocationAPI
import com.example.tomas.carsecurity.storage.entity.Location
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LocationController(serverUrl: String, httpClient: OkHttpClient) {

    private val tag = "LocationController"
    private val locationAPI: LocationAPI

    init {
        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        locationAPI = retrofit.create(LocationAPI::class.java)
    }

    fun createLocations(locations: List<Location>): Response<Void> {
        val method = locationAPI.createLocations(locations)

        Log.d(tag, "Sending message to create locations endpoint. URL: ${method.request().url()}")
        return try {
            method.execute()
        } catch (e: Exception) {
            Log.d(tag, "Can not send request. Exception: $e")
            Response.error(408, ResponseBody.create(null, ""))
        }
    }
}