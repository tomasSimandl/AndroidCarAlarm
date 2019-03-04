package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.POST

interface FirebaseAPI {

    @POST(Mapping.FIREBASE_TOKEN_URL)
    fun saveToken(
            @Field("car_id") carId: Long,
            @Field("token") token: String
    ): Call<Any>
}