package com.example.tomas.carsecurity.storage.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import android.location.Location
import com.google.gson.annotations.SerializedName


@Entity(tableName = "location",
        foreignKeys = [ForeignKey(entity = Route::class,
                parentColumns = arrayOf("uid"),
                childColumns = arrayOf("local_route_id"),
                onDelete = ForeignKey.CASCADE)])
data class Location(

        @PrimaryKey(autoGenerate = true)
        var uid: Int = 0,

        @ColumnInfo(name = "latitude")
        var latitude: Double = 0.0,

        @ColumnInfo(name = "longitude")
        var longitude: Double = 0.0,

        @ColumnInfo(name = "altitude")
        var altitude: Double = 0.0,

        @ColumnInfo(name = "time")
        var time: Long = 0L,

        @ColumnInfo(name = "accuracy")
        var accuracy: Float = 0F,

        @ColumnInfo(name = "speed")
        var speed: Float = 0F,

        @SerializedName("route_id")
        @ColumnInfo(name = "route_id")
        var routeId: Long? = null,

        @ColumnInfo(name = "local_route_id")
        @Transient // Ignored in Retrofit but not in Room
        var localRouteId: Int? = null

) {
    constructor(location: Location, localRouteId: Int? = null) : this() {
        latitude = location.latitude
        longitude = location.longitude
        altitude = location.altitude
        time = location.time
        accuracy = location.accuracy
        speed = location.speed
        this.localRouteId = localRouteId
    }
}