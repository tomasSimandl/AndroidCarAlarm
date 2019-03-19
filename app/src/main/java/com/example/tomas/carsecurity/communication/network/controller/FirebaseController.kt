package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.FirebaseAPI
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Provide communication with server Firebase endpoint over Retrofit API.
 *
 * @param serverUrl url address of requested server
 * @param httpClient configured client over which can communicate with server. Authorization to
 *                  server should be already configured.
 */
class FirebaseController(serverUrl: String, httpClient: OkHttpClient) {

    /** Logger tag */
    private val tag = "FirebaseController"
    /** Retrofit API for communication over Firebase endpoint */
    private val firebaseAPI: FirebaseAPI

    /**
     * Initialization of Retrofit API for communication over Firebase endpoint.
     */
    init {
        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        firebaseAPI = retrofit.create(FirebaseAPI::class.java)
    }

    /**
     * Endpoint for saving Firebase token of this device to server.
     *
     * @param carId car id which is associated with this device.
     * @param token Firebase token of this device over which can server target this device.
     * @return on success return status code 201, otherwise json with error message.
     *          All responses are wrapped in Response object.
     */
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