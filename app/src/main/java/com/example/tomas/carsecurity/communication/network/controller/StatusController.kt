package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.EventAPI
import com.example.tomas.carsecurity.communication.network.api.StatusAPI
import com.example.tomas.carsecurity.communication.network.dto.StatusCreate
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StatusController(serverUrl: String, httpClient: OkHttpClient) {

    private val tag = "StatusController"
    private val statusAPI: StatusAPI

    init {
        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        statusAPI = retrofit.create(StatusAPI::class.java)
    }

    fun createStatus(status: StatusCreate): Response<Void> {
        val method = statusAPI.createStatus(status)

        Log.d(tag, "Sending message to create status endpoint. URL: ${method.request().url()}")
        return try {
            method.execute()
        } catch (e: Exception) {
            Log.d(tag, "Can not send request. Exception: ${e.printStackTrace()}")
            Response.error(408, ResponseBody.create(null, ""))
        }
    }
}