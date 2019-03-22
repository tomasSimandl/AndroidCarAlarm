package com.example.tomas.carsecurity.storage.dao

import android.arch.persistence.room.*
import com.example.tomas.carsecurity.storage.entity.Route

/**
 * Data access object to table route in Room database.
 */
@Dao
interface RouteDao {

    /**
     * Method returns all routes stored in database.
     * @return list of all routes.
     */
    @Query("SELECT * FROM route")
    fun getAll(): List<Route>

    /**
     * Method return specific route from database.
     * @param routeId database id of requested route
     * @return One [Route] from database
     */
    @Query("SELECT * FROM route WHERE uid=:routeId")
    fun get(routeId: Int): Route

    /**
     * Method insert one route to database.
     * @param route which will be inserted to database.
     * @return database id of inserted route
     */
    @Insert
    fun insert(route: Route): Long

    /**
     * Method delete all input routes from database.
     * @param route vararg of all routes which will be deleted.
     */
    @Delete
    fun delete(vararg route: Route)

    /**
     * Method delete input routes from database.
     * @param routes is list of routes which will be deleted from database.
     */
    @Delete
    fun delete(routes: List<Route>)

    /**
     * Method update input route in database. Route must already exits in database.
     * @param route which will be updated.
     */
    @Update
    fun update(route: Route)
}