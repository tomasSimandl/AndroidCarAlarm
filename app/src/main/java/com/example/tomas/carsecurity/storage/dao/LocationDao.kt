package com.example.tomas.carsecurity.storage.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.example.tomas.carsecurity.storage.entity.Location

@Dao
interface LocationDao {

    @Query("SELECT * FROM location")
    fun getAll(): List<Location>

    @Insert
    fun insert(location: Location)

    @Query("DELETE FROM location")
    fun deleteAll()
}