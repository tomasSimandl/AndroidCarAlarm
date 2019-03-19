package com.example.tomas.carsecurity.communication.network.config

import android.content.Context
import android.util.Log
import com.example.tomas.carsecurity.communication.network.controller.UserController
import com.example.tomas.carsecurity.communication.network.dto.Token
import com.example.tomas.carsecurity.storage.Storage
import com.google.gson.internal.LinkedTreeMap
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.*

/**
 * Class represent automatic OAuth token refresh logic.
 *
 * @param authUrl url of authorization server
 * @param context application context used for opening of Room database.
 */
class TokenAuthenticator(private val authUrl: String, private val context: Context) : Authenticator {

    /** Logger tag */
    private val tag = "TokenAuthenticator"

    /**
     * When any request response with status code 401 Unauthorized, method find user in Room
     * database and send refresh token request to OAuth 2.0 Authorization server. When refresh
     * token request is successful send again original request to server.
     *
     * @return created request or null when response code is different than 401.
     */
    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {

        try {
            if (response.code() == 401) {
                val user = Storage.getInstance(context).userService.getUser()

                if (user != null) {
                    val refreshResponse = UserController(authUrl, context).refreshToken(user.refreshToken)

                    if (refreshResponse.isSuccessful) {
                        val token = Token(refreshResponse.body() as LinkedTreeMap<*, *>)

                        user.accessToken = token.accessToken
                        user.tokenType = token.tokenType
                        user.refreshToken = token.refreshToken
                        user.scope = token.scope
                        user.expiresAt = Date().time + token.expiresIn
                        Storage.getInstance(context).userService.updateUser(user)

                        return response.request().newBuilder()
                                .header("Authorization", "${user.tokenType} ${user.accessToken}")
                                .build()
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Can not refresh token. Message: ${e.message}")
        }
        return null
    }
}