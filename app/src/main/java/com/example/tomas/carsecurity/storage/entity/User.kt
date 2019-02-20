package com.example.tomas.carsecurity.storage.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.example.tomas.carsecurity.communication.network.dto.Token


@Entity(tableName = "user")
data class User(

        @PrimaryKey
        @ColumnInfo(name = "username")
        var username: String,

        @ColumnInfo(name = "access_token")
        var accessToken: String,

        @ColumnInfo(name = "token_type")
        var tokenType: String,

        @ColumnInfo(name = "refresh_token")
        var refreshToken: String,

        @ColumnInfo(name = "expires_at")
        var expiresAt: Long,

        @ColumnInfo(name = "scope")
        var scope: String,

        @ColumnInfo(name = "car_name")
        var carName: String = "",

        @ColumnInfo(name = "car_id")
        var carId: Long = -1
) {

        constructor(token: Token, username: String, actualLongTime: Long): this (
                username = username,
                accessToken = token.accessToken,
                tokenType = token.tokenType,
                refreshToken = token.refreshToken,
                expiresAt = actualLongTime + token.expiresIn,
                scope = token.scope
        )
}