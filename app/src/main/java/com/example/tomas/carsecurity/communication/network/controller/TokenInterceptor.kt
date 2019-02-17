package com.example.tomas.carsecurity.communication.network.controller

import android.content.Context
import com.example.tomas.carsecurity.storage.Storage
import okhttp3.Interceptor
import okhttp3.Response

class TokenInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()

        val user = Storage.getInstance(context).userService.getUser()

        if(user != null){
            builder.header("Authorization", "${user.tokenType} ${user.accessToken}")
        }

        return chain.proceed(builder.build())
    }
}