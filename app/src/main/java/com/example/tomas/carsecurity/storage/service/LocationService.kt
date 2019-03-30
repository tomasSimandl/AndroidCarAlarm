package com.example.tomas.carsecurity.storage.service

import com.example.tomas.carsecurity.storage.AppDatabase
import com.example.tomas.carsecurity.storage.entity.Location

/**
 * Service which only call [database] dao objects associated with location table.
 * @param database is open Room database.
 */
class LocationService(private val database: AppDatabase) {

    /**
     * Method store input location in database.
     * @param location which will be stored in database.
     */
    fun saveLocation(location: Location) {
        database.locationDao().insert(location)
    }

    /**
     * Method update input location in database. Location must already exists in database.
     * @param location which will be stored in database.
     */
    fun updateLocation(location: Location) {
        database.locationDao().update(location)
    }

    /**
     * Return all locations associate with route specified with [routeId].
     * @param routeId id of route of which locations will be returned.
     * @return all locations of input route.
     */
    fun getLocationsByLocalRouteId(routeId: Int?): List<Location> {
        return database.locationDao().getAllByLocalRouteId(routeId)
    }

    /**
     * Method return number of all locations in database.
     * @return number of locations in database
     */
    fun countLocations(): Long {
        return database.locationDao().count()
    }

    /**
     * Delete all locations given by input list from database.
     * @param locations list of location which will be deleted from database.
     */
    fun deleteLocations(locations: List<Location>) {
        database.locationDao().delete(locations)
    }
}