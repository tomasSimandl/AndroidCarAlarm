package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.LocationAPI
import com.example.tomas.carsecurity.storage.entity.Location
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Provide communication with server over location endpoint over Retrofit API.
 *
 * @param serverUrl url address of requested server
 * @param httpClient configured client over which can communicate with server. Authorization to
 *                  server should be already configured.
 */
class LocationController(serverUrl: String, httpClient: OkHttpClient) {

    /** Logger tag. */
    private val tag = "LocationController"
    /** Retrofit API for communication over Location endpoint. */
    private val locationAPI: LocationAPI

    /**
     * Initialization of Retrofit API for communication over Location endpoint.
     */
    init {
        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        locationAPI = retrofit.create(LocationAPI::class.java)
    }

    /**
     * Endpoint for creating of new locations on server.
     *
     * @param locations list of locations which will be saved to server.
     * @return on success return status code 201, otherwise json with error message.
     *          All responses are wrapped in Response object.
     */
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