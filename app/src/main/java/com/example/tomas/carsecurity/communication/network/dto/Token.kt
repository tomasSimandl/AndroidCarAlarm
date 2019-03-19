package com.example.tomas.carsecurity.communication.network.dto

import com.google.gson.internal.LinkedTreeMap

/**
 * Class represents response on login or token refresh request. Attributes are standardized by
 * OAuth 2.0
 */
data class Token(
        /** OAuth access token used for user authorization. */
        var accessToken: String = "",

        /** Type of token. In this case should be always Barer. */
        var tokenType: String = "",

        /** Refresh token which is used when access token expires. */
        var refreshToken: String = "",

        /** Number of second when access token expires. */
        var expiresIn: Long = 0,

        /** Given scope for login user. */
        var scope: String = ""
) {

    /**
     * Constructor which extract from input LinkedTreeMap attributes of this class. Presents of
     * requested attributes is not checked.
     *
     * @param data is Map which contains all attributes defined in this class.
     */
    constructor(data: LinkedTreeMap<*, *>) : this() {
        accessToken = data["access_token"] as String
        tokenType = data["token_type"] as String
        refreshToken = data["refresh_token"] as String
        expiresIn = (data["expires_in"] as Double).toLong()
        scope = data["scope"] as String
    }
}