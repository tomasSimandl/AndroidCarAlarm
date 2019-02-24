package com.example.tomas.carsecurity.storage.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey


@Entity(tableName = "route")
data class Route (

        @PrimaryKey(autoGenerate = true)
        var uid: Int = 0,

        @ColumnInfo(name = "route_id")
        var serverRouteId: Long? = null,

        @ColumnInfo(name = "car_id")
        var carId: Long = 0,

        @ColumnInfo(name = "finished")
        var finished: Boolean = false
)