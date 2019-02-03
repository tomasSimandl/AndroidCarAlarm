package com.example.tomas.carsecurity.storage.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.example.tomas.carsecurity.storage.entity.Location
import android.arch.persistence.room.Delete

@Dao
interface LocationDao {

    @Query("SELECT * FROM location")
    fun getAll(): List<Location>

    @Query("SELECT * FROM location LIMIT :limit")
    fun getAll(limit: Int): List<Location>

    @Query("SELECT * FROM location WHERE local_route_id = :routeId")
    fun getAllByRouteId(routeId: Int): List<Location>

    @Insert
    fun insert(location: Location)

    @Delete
    fun delete(vararg location: Location)

    @Delete
    fun delete(locations: List<Location>)

    @Query("DELETE FROM location")
    fun deleteAll()
}