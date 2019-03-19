package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.RouteAPI
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Provide communication with server route endpoint over Retrofit API.
 *
 * @param serverUrl url address of requested server
 * @param httpClient configured client over which can communicate with server. Authorization to
 *                  server should be already configured.
 */
class RouteController(serverUrl: String, httpClient: OkHttpClient) {

    /** Logger tag */
    private val tag = "RouteController"
    /** Retrofit API for communication over Route endpoint */
    private val routeAPI: RouteAPI

    /**
     * Initialization of Retrofit API for communication over Route endpoint
     */
    init {
        Log.d(tag, "Initializing")

        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        routeAPI = retrofit.create(RouteAPI::class.java)
    }

    /**
     * Endpoint for creating new route on server.
     *
     * @param carId car id of car which created the route.
     * @param routeStartTime time when route started. (milliseconds of Epoch)
     * @return on success return status code 201 and json with created route id,
     *          otherwise json with error message. All responses are wrapped in Response object.
     */
    fun createRoute(carId: Long, routeStartTime: Long): Response<Any> {
        val method = routeAPI.createRoute(carId, routeStartTime)

        Log.d(tag, "Sending message to create route endpoint. URL: ${method.request().url()}")
        return try {
            method.execute()
        } catch (e: Exception) {
            Log.d(tag, "Can not send request. Exception: $e")
            Response.error(408, ResponseBody.create(null, ""))
        }
    }
}