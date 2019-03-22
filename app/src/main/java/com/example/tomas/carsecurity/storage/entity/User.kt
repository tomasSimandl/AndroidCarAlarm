package com.example.tomas.carsecurity.storage.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.example.tomas.carsecurity.communication.network.NetworkProvider
import com.example.tomas.carsecurity.communication.network.dto.Token

/**
 * Data class represents table user in Room database.
 */
@Entity(tableName = "user")
data class User(

        /** Unique username of user */
        @PrimaryKey
        @ColumnInfo(name = "username")
        var username: String,

        /** OAuth 2.0 users access token for [NetworkProvider] */
        @ColumnInfo(name = "access_token")
        var accessToken: String,

        /** Type of OAuth 2.0 token for [NetworkProvider] */
        @ColumnInfo(name = "token_type")
        var tokenType: String,

        /** OAuth 2.0 refresh token for [NetworkProvider] */
        @ColumnInfo(name = "refresh_token")
        var refreshToken: String,

        /** Time when OAuth 2.0 token expires. Used in [NetworkProvider]. Millis since 1.1.1970 */
        @ColumnInfo(name = "expires_at")
        var expiresAt: Long,

        /** Approved access scope for OAuth 2.0 in [NetworkProvider]. */
        @ColumnInfo(name = "scope")
        var scope: String,

        /** Name of car to which is user actually login */
        @ColumnInfo(name = "car_name")
        var carName: String = "",

        /** Identification of car on remote server to which is user actually login. */
        @ColumnInfo(name = "car_id")
        var carId: Long = -1
) {
    /**
     * @param token is OAuth login response from authorization server.
     * @param username of actual logged user.
     * @param actualLongTime is time when [token] was received. Millis since 1.1.1970
     */
    constructor(token: Token, username: String, actualLongTime: Long) : this(
            username = username,
            accessToken = token.accessToken,
            tokenType = token.tokenType,
            refreshToken = token.refreshToken,
            expiresAt = actualLongTime + token.expiresIn,
            scope = token.scope
    )
}