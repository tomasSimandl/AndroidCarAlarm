package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface UserAPI {

    @FormUrlEncoded
    @POST(Mapping.LOGIN_URL)
    fun login(
            @Header("Authorization") credentials: String,
            @Field("grant_type") grantType: String,
            @Field("username") username: String,
            @Field("password") password: String,
            @Field("scope") scope: String
    ): Call<Any>

    @FormUrlEncoded
    @POST(Mapping.LOGIN_URL)
    fun refreshToken(
            @Header("Authorization") credentials: String,
            @Field("grant_type") grantType: String,
            @Field("refresh_token") refreshToken: String
    ): Call<Any>
}