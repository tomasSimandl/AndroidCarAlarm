package com.example.tomas.carsecurity.storage.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import android.location.Location
import com.google.gson.annotations.SerializedName

/**
 * Data class represents table location in Room database.
 */
@Entity(tableName = "location",
        foreignKeys = [ForeignKey(entity = Route::class,
                parentColumns = arrayOf("uid"),
                childColumns = arrayOf("local_route_id"),
                onDelete = ForeignKey.CASCADE)])
data class Location(

        /** Id which identifies location in database */
        @PrimaryKey(autoGenerate = true)
        var uid: Int = 0,

        /** Latitude of record */
        @ColumnInfo(name = "latitude")
        var latitude: Double = 0.0,

        /** Longitude of record */
        @ColumnInfo(name = "longitude")
        var longitude: Double = 0.0,

        /** Altitude of record */
        @ColumnInfo(name = "altitude")
        var altitude: Double = 0.0,

        /** Time when was record recorded. Millis since 1.1.1970 */
        @ColumnInfo(name = "time")
        var time: Long = 0L,

        /** Sensor accuracy of record */
        @ColumnInfo(name = "accuracy")
        var accuracy: Float = 0F,

        /** Actual speed when record was created */
        @ColumnInfo(name = "speed")
        var speed: Float = 0F,

        /** Distance from last record */
        @ColumnInfo(name = "distance")
        var distance: Float = 0F,

        /** Identification number of route on remote server. */
        @SerializedName("route_id")
        @ColumnInfo(name = "route_id")
        var routeId: Long? = null,

        /** Identification number of route in local database. */
        @ColumnInfo(name = "local_route_id")
        @Transient // Ignored in Retrofit but not in Room
        var localRouteId: Int? = null

) {
    /**
     * @param location recorded location returned from sensor.
     * @param localRouteId is identification number of route in local database.
     * @param distance from last recorded location.
     */
    constructor(location: Location, localRouteId: Int? = null, distance: Float = 0F) : this(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            time = location.time,
            accuracy = location.accuracy,
            speed = location.speed,
            localRouteId = localRouteId,
            distance = distance
    )
}