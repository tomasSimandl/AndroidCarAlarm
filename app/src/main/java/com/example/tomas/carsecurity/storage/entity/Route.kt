package com.example.tomas.carsecurity.storage.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import java.util.*


/**
 * Data class represents table route in Room database.
 */
@Entity(tableName = "route")
data class Route @Ignore constructor(

        /** Id which identifies route in database */
        @PrimaryKey(autoGenerate = true)
        var uid: Int = 0,

        /** Id which identifies route on remote server. */
        @ColumnInfo(name = "route_id")
        var serverRouteId: Long? = null,

        /** Id of car on remote server */
        @ColumnInfo(name = "car_id")
        var carId: Long = 0,

        /** Time when route was created. Millis since 1.1.1970 */
        @ColumnInfo(name = "time")
        var time: Long = Date().time
) {
    constructor() : this(0, null, 0, Date().time)
}