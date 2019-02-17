package com.example.tomas.carsecurity.communication.network.dto

import com.google.gson.internal.LinkedTreeMap

data class Token(
        var accessToken: String = "",
        var tokenType: String = "",
        var refreshToken: String = "",
        var expiresIn: Long = 0,
        var scope: String = ""
) {

    constructor(data: LinkedTreeMap<*, *>): this() {
        accessToken = data["access_token"] as String
        tokenType = data["token_type"] as String
        refreshToken = data["refresh_token"] as String
        expiresIn = (data["expires_in"] as Double).toLong()
        scope = data["scope"] as String
    }
}