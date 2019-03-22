package com.example.tomas.carsecurity.storage.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Data class represents table message in Room database.
 */
@Entity(tableName = "message")
data class Message(

        /** Id which identifies message in database */
        @PrimaryKey(autoGenerate = true)
        var uid: Int = 0,

        /** Hash of communication provider over which should be this message send. */
        @ColumnInfo(name = "communicator")
        var communicatorHash: Int,

        /** Body of message */
        @ColumnInfo(name = "body")
        var message: String
)