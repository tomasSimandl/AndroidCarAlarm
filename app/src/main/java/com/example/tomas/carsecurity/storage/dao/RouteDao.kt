package com.example.tomas.carsecurity.storage.dao

import android.arch.persistence.room.*
import com.example.tomas.carsecurity.storage.entity.Route

@Dao
interface RouteDao {

    @Query("SELECT * FROM route")
    fun getAll(): List<Route>

    @Insert
    fun insert(route: Route)

    @Delete
    fun delete(vararg route: Route)

    @Delete
    fun delete(routes: List<Route>)

    @Update
    fun update(route: Route)
}