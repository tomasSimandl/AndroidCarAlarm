package com.example.tomas.carsecurity.storage.dao

import android.arch.persistence.room.*
import com.example.tomas.carsecurity.storage.entity.Location

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

    @Update
    fun update(location: Location)

    @Delete
    fun delete(vararg location: Location)

    @Delete
    fun delete(locations: List<Location>)

    @Query("DELETE FROM location")
    fun deleteAll()
}