package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.CarAPI
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Provide communication with server car endpoint over Retrofit API.
 *
 * @param serverUrl url address of requested server
 * @param httpClient configured client over which can communicate with server. Authorization to
 *                  server should be already configured.
 */
class CarController(serverUrl: String, httpClient: OkHttpClient) {

    /** Logger tag */
    private val tag = "CarController"
    /** Retrofit API for communication over Car endpoint */
    private val carAPI: CarAPI

    /**
     * Initialization of Retrofit API for communication over Car endpoint
     */
    init {
        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        carAPI = retrofit.create(CarAPI::class.java)
    }

    /**
     * Endpoint for getting all logged users cars.
     *
     * @return Collection of cars wrapped in Response object.
     */
    fun getCars(): Response<Any> {
        val method = carAPI.getCars()

        Log.d(tag, "Sending message to get cars endpoint. URL: ${method.request().url()}")
        return try {
            method.execute()
        } catch (e: Exception) {
            Log.d(tag, "Can not send request. Exception: ${e.printStackTrace()}")
            Response.error(408, ResponseBody.create(null, ""))
        }
    }

    /**
     * Endpoint for creating new cars on server.
     *
     * @param name of created car.
     * @return on success return status code 201 and json with created car id,
     *          otherwise json with error message. All responses are wrapped in Response object.
     */
    fun createCar(name: String): Response<Any> {

        val json = JsonObject()
        json.addProperty("name", name)
        json.addProperty("icon", "")

        val requestBody = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), json.toString())

        val method = carAPI.createCar(requestBody)
        Log.d(tag, "Sending message to create car endpoint. URL: ${method.request().url()}")
        return try {
            method.execute()
        } catch (e: Exception) {
            Log.d(tag, "Can not send request. Exception: ${e.printStackTrace()}")
            Response.error(408, ResponseBody.create(null, ""))
        }
    }
}