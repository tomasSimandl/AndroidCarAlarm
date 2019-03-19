package com.example.tomas.carsecurity.communication.network.config

import android.content.Context
import com.example.tomas.carsecurity.storage.Storage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * This class is used for adding OAuth 2.0 access token to every request.
 *
 * @param context application context which is used for opening Room database.
 */
class TokenInterceptor(private val context: Context) : Interceptor {

    /**
     * Method get users OAuth 2.0 access token and added it to request header. Request is taken from
     * input Chain.
     *
     * @return created response with Authorization header or original request when user is not login.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()

        val user = Storage.getInstance(context).userService.getUser()

        if (user != null) {
            builder.header("Authorization", "${user.tokenType} ${user.accessToken}")
        }

        return chain.proceed(builder.build())
    }
}