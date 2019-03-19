package com.example.tomas.carsecurity.communication.network.api

import com.example.tomas.carsecurity.communication.network.Mapping
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit API for outgoing network requests related to User.
 */
interface UserAPI {

    /**
     * Prepare login request to server.
     *
     * @param credentials barer authorization
     * @param grantType request grant type by OAuth 2.0
     * @param username of user
     * @param password of user
     * @param scope when logged user request an access
     */
    @FormUrlEncoded
    @POST(Mapping.LOGIN_URL)
    fun login(
            @Header("Authorization") credentials: String,
            @Field("grant_type") grantType: String,
            @Field("username") username: String,
            @Field("password") password: String,
            @Field("scope") scope: String
    ): Call<Any>

    /**
     * Prepare refresh OAuth token request to server.
     *
     * @param credentials barer authorization
     * @param grantType requested grant type by OAuth 2.0
     * @param refreshToken OAuth 2.0 refresh token given by server at login
     */
    @FormUrlEncoded
    @POST(Mapping.LOGIN_URL)
    fun refreshToken(
            @Header("Authorization") credentials: String,
            @Field("grant_type") grantType: String,
            @Field("refresh_token") refreshToken: String
    ): Call<Any>
}