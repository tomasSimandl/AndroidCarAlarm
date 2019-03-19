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

/**
 * Provide communication with servers status endpoint over Retrofit API.
 *
 * @param serverUrl url address of requested server
 * @param httpClient configured client over which can communicate with server. Authorization to
 *                  server should be already configured.
 */
class StatusController(serverUrl: String, httpClient: OkHttpClient) {

    /** Logger tag. */
    private val tag = "StatusController"
    /** Retrofit API for communication over Status endpoint. */
    private val statusAPI: StatusAPI

    /**
     * Initialization of Retrofit API for communication over Status endpoint.
     */
    init {
        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        statusAPI = retrofit.create(StatusAPI::class.java)
    }

    /**
     * Endpoint for sending status to server.
     *
     * @param status object which reperesent new status which will be sent to server.
     * @return on success return status code 201, otherwise json with error message.
     *          All responses are wrapped in Response object.
     */
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