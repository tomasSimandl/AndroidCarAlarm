package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.CarAPI
import com.example.tomas.carsecurity.communication.network.api.EventAPI
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CarController(serverUrl: String, httpClient: OkHttpClient) {

    private val tag = "CarController"
    private val carAPI: CarAPI

    init {
        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        carAPI = retrofit.create(CarAPI::class.java)
    }

    fun getCars(): Response<Any> {
        val method = carAPI.getCars()

        Log.d(tag, "Sending message to get cars endpoint. URL: ${method.request().url()}")
        return method.execute()
    }

    fun createCar(name: String): Response<Any> {

        val json = JsonObject()
        json.addProperty("name", name)
        json.addProperty("icon", "")

        val requestBody = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), json.toString())

        val method = carAPI.createCar(requestBody)
        Log.d(tag, "Sending message to create car endpoint. URL: ${method.request().url()}")
        return method.execute()
    }
}