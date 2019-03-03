package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import com.example.tomas.carsecurity.communication.network.dto.EventCreate
import com.example.tomas.carsecurity.communication.network.dto.StatusCreate
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface StatusAPI {

    @POST(Mapping.STATUS_URL)
    fun createStatus(@Body status: StatusCreate): Call<Void>
}