package com.example.tomas.carsecurity.storage.dao

import android.arch.persistence.room.*
import android.support.annotation.RequiresPermission
import com.example.tomas.carsecurity.storage.entity.Route

@Dao
interface RouteDao {

    @Query("SELECT * FROM route")
    fun getAll(): List<Route>

    @Query("SELECT * FROM route WHERE uid=:routeId")
    fun get(routeId: Int): Route

    @Insert
    fun insert(route: Route)

    @Delete
    fun delete(vararg route: Route)

    @Delete
    fun delete(routes: List<Route>)

    @Update
    fun update(route: Route)
}