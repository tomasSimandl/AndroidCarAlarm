package com.example.tomas.carsecurity.communication.network.controller

import android.content.Context
import android.util.Log
import com.example.tomas.carsecurity.communication.network.api.UserAPI
import okhttp3.Credentials
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

/**
 * Provide communication with servers user endpoint over Retrofit API.
 *
 * @param serverUrl url address of requested authorization server
 */
class UserController(serverUrl: String, context: Context) {

    /** Logger tag */
    private val tag = "UserController"
    /** Retrofit API for communication over User endpoint. */
    private val userAPI: UserAPI

    /** OAuth client id of this application configured by config.properties file. */
    private val clientId: String
    /** OAuth client secret of this application configured in config.properties file. */
    private val clientSecret: String

    /**
     * Constructor load client.id and client.secret from property file and initialize Retrofit API.
     *
     * @throws IOException when properties file can not be open.
     */
    init {
        val properties = Properties()
        properties.load(context.assets.open("config.properties"))
        clientId = properties["oauth.client.id"] as String
        clientSecret = properties["oauth.client.secret"] as String

        Log.d(tag, "Initializing")
        val retrofit = Retrofit.Builder()
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        userAPI = retrofit.create(UserAPI::class.java)
    }

    /**
     * Endpoint for sending login request to authorization server. For authorization is used
     * password Grant Type.
     *
     * @param username of user
     * @param password of user
     * @return on success (200 OK) response contains OAuth 2.0 token.
     */
    fun login(username: String, password: String): Response<Any> {
        val loginGrantType = "password"
        val scope = "read write"

        val credentials = Credentials.basic(clientId, clientSecret)
        val method = userAPI.login(credentials, loginGrantType, username, password, scope)

        Log.d(tag, "Sending message to login endpoint: ${method.request()}")
        return method.execute()
    }

    /**
     * Endpoint for sending refresh OAuth token request to authorization server.
     *
     * @param refreshToken OAuth refresh token which was given by authorization server on login.
     * @return on success (200 OK) response contains OAuth 2.0 token.
     */
    fun refreshToken(refreshToken: String): Response<Any> {
        val refreshGrantType = "refresh_token"

        val credentials = Credentials.basic(clientId, clientSecret)
        val method = userAPI.refreshToken(credentials, refreshGrantType, refreshToken)

        Log.d(tag, "Sending message to refresh token endpoint. URL: ${method.request().url()}")
        return method.execute()
    }
}