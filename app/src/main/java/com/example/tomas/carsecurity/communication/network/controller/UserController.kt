package com.example.tomas.carsecurity.communication.network.controller

import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.UserAPI
import okhttp3.Credentials
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserController(serverUrl: String) {

    private val tag = "UserController"
    private val userAPI: UserAPI

    private val CLIENT_ID = "mobile-app-client"
    private val CLIENT_SECRET = "secret"
    private val LOGIN_GRANT_TYPE = "password"
    private val REFRESH_GRANT_TYPE = "password"
    private val SCOPE = "read write"

    init {
        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        userAPI = retrofit.create(UserAPI::class.java)
    }

    fun login(username: String, password: String): Response<Any>{

        val credentials = Credentials.basic(CLIENT_ID, CLIENT_SECRET)
        val method = userAPI.login(credentials, LOGIN_GRANT_TYPE, username, password, SCOPE)

        Log.d(tag, "Sending message to login endpoint: ${method.request()}")
        return method.execute()
    }

    fun refreshToken(refreshToken: String) : Response<Any> {
        val credentials = Credentials.basic(CLIENT_ID, CLIENT_SECRET)
        val method = userAPI.refreshToken(credentials, REFRESH_GRANT_TYPE, refreshToken)

        Log.d(tag, "Sending message to refresh token endpoint. URL: ${method.request().url()}")
        return method.execute()
    }
}