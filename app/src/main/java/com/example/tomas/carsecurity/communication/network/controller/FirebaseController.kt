package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.FirebaseAPI
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FirebaseController(serverUrl: String, httpClient: OkHttpClient) {

    private val tag = "FirebaseController"
    private val firebaseAPI: FirebaseAPI

    init {
        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        firebaseAPI = retrofit.create(FirebaseAPI::class.java)
    }

    fun saveToken(carId: Long, token: String): Response<Void> {
        val method = firebaseAPI.saveToken(carId, token)

        Log.d(tag, "Sending message to save Firebase token endpoint. URL: ${method.request().url()}")
        return try {
            method.execute()
        } catch (e: Exception) {
            Log.d(tag, "Can not send request. Exception: ${e.printStackTrace()}")
            Response.error(408, ResponseBody.create(null, ""))
        }
    }
}