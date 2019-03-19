package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Retrofit API for outgoing network requests related to Firebase but not send to Firebase.
 */
interface FirebaseAPI {

    /**
     * Prepare request to server which save input token to car with given id.
     */
    @FormUrlEncoded
    @POST(Mapping.FIREBASE_TOKEN_URL)
    fun saveToken(
            @Field("car_id") carId: Long,
            @Field("token") token: String
    ): Call<Void>
}