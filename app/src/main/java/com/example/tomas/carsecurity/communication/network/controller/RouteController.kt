package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.RouteAPI
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RouteController(serverUrl: String, httpClient: OkHttpClient) {

    private val tag = "RouteController"
    private val routeAPI: RouteAPI

    init {
        Log.d(tag, "Initializing")

        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()


        routeAPI = retrofit.create(RouteAPI::class.java)
    }

    fun createRoute(carId: Long): Response<Any> {
        val method = routeAPI.createRoute(carId)

        Log.d(tag, "Sending message to create route endpoint. URL: ${method.request().url()}")
        return try {
            method.execute()
        } catch (e: Exception) {
            Log.d(tag, "Can not send request. Exception: $e")
            Response.error(418, ResponseBody.create(null, ""))
        }
    }
}