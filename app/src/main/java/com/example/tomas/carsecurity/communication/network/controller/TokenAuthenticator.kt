package com.example.tomas.carsecurity.communication.network.controller

import android.content.Context
import android.util.Log
import com.example.tomas.carsecurity.communication.network.dto.Token
import com.example.tomas.carsecurity.storage.Storage
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.*


class TokenAuthenticator(private val authUrl: String, private val context: Context) : Authenticator {
    private val tag = "TokenAuthenticator"

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {

        try {
            if (response.code() == 401) {
                val user = Storage.getInstance(context).userService.getUser()

                if (user != null) {
                    val refreshResponse = UserController(authUrl).refreshToken(user.refreshToken)

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