package com.example.tomas.carsecurity.communication.network.controller

import com.example.tomas.carsecurity.communication.network.api.RouteAPI
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RouteController(serverUrl: String) : Callback<String> {

    val routeAPI: RouteAPI

    init {
        val retrofit = Retrofit.Builder()
                    .baseUrl(serverUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

        routeAPI = retrofit.create(RouteAPI::class.java)
    }

    public fun createRoute(carId: Long){
        routeAPI.createRoute(carId).enqueue(this) // Asynchronous
        //routeAPI.createRoute(carId).execute()  // Synchronous
    }


    override fun onResponse(call: Call<String>, response: Response<String>) {
        if(response.isSuccessful) {
            println("=============================================================================")
            println("Message: ${response.body()}")
            println("=============================================================================")
        } else {
            println("=============================================================================")
            println("Error message: ${response.errorBody()}")
            println("=============================================================================")
        }
    }

    override fun onFailure(call: Call<String>, t: Throwable) {
        println("=============================================================================")
        println("Failure: ${t.message}")
        println("=============================================================================")
    }
}