package com.example.tomas.carsecurity.storage.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.location.Location


@Entity(tableName = "location")
class Location() {

    constructor(location: Location) : this() {
        latitude = location.latitude
        longitude = location.longitude
        altitude = location.altitude
        time = location.time
        accuracy = location.accuracy
        speed = location.speed
    }

    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @ColumnInfo(name = "latitude")
    var latitude: Double = 0.0

    @ColumnInfo(name = "longitude")
    var longitude: Double = 0.0

    @ColumnInfo(name = "altitude")
    var altitude: Double = 0.0

    @ColumnInfo(name = "time")
    var time: Long = 0L

    @ColumnInfo(name = "accuracy")
    var accuracy: Float = 0F

    @ColumnInfo(name = "speed")
    var speed: Float = 0F
}