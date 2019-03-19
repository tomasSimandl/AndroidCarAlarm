package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.EventAPI
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Provide communication with server event endpoint over Retrofit API.
 *
 * @param serverUrl url address of requested server
 * @param httpClient configured client over which can communicate with server. Authorization to
 *                  server should be already configured.
 */
class EventController(serverUrl: String, httpClient: OkHttpClient) {

    /** Logger tag */
    private val tag = "EventController"
    /** Retrofit API for communication over Event endpoint */
    private val eventAPI: EventAPI

    /**
     * Initialization of Retrofit API for communication over Event endpoint
     */
    init {
        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        eventAPI = retrofit.create(EventAPI::class.java)
    }

    /**
     * Endpoint for creating new event on server.
     *
     * @param event object which represents object to create.
     * @return on success return status code 201, otherwise json with error message.
     *          All responses are wrapped in Response object.
     */
    fun createEvent(event: String): Response<Void> {
        val requestBody = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), event)
        val method = eventAPI.createEvent(requestBody)

        Log.d(tag, "Sending message to create event endpoint. URL: ${method.request().url()}")
        return try {
            method.execute()
        } catch (e: Exception) {
            Log.d(tag, "Can not send request. Exception: ${e.printStackTrace()}")
            Response.error(408, ResponseBody.create(null, ""))
        }
    }
}