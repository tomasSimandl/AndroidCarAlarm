package com.example.tomas.carsecurity.storage.dao

import android.arch.persistence.room.*
import com.example.tomas.carsecurity.storage.entity.Location

/**
 * Data access object to table location to Room database.
 */
@Dao
interface LocationDao {

    /**
     * Return all locations from database.
     * @return list of all locations.
     */
    @Query("SELECT * FROM location")
    fun getAll(): List<Location>

    /**
     * Return locations from database. Maximal number of locations is specified by [limit].
     * @param limit maximal number of locations returned by this method.
     * @return list of locations from database.
     */
    @Query("SELECT * FROM location LIMIT :limit")
    fun getAll(limit: Int): List<Location>

    /**
     * Return all locations associate with route specified with [routeId].
     * @param routeId id of route of which locations will be returned.
     * @return all locations of input route.
     */
    @Query("SELECT * FROM location WHERE local_route_id is :routeId")
    fun getAllByLocalRouteId(routeId: Int?): List<Location>

    /**
     * Method store input location in database.
     * @param location which will be stored in database.
     */
    @Insert
    fun insert(location: Location)

    /**
     * Method update input location in database. Location must already exists in database.
     * @param location which will be stored in database.
     */
    @Update
    fun update(location: Location)

    /**
     * Method delete all locations given by input locations from database.
     * @param location vararg of locations which will be deleted from database.
     */
    @Delete
    fun delete(vararg location: Location)

    /**
     * Delete all locations given by input list from database.
     * @param locations list of location which will be deleted from database.
     */
    @Delete
    fun delete(locations: List<Location>)

    /**
     * Method delete all location from database.
     */
    @Query("DELETE FROM location")
    fun deleteAll()
}