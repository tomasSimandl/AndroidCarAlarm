package com.example.tomas.carsecurity.storage.service

import com.example.tomas.carsecurity.storage.AppDatabase
import com.example.tomas.carsecurity.storage.entity.Location

class LocationService(private val database: AppDatabase) {

    fun saveLocation(location: Location) {
        database.locationDao().insert(location)
    }

    fun updateLocation(location: Location) {
        database.locationDao().update(location)
    }

    fun getLocations(): List<Location> {
        return database.locationDao().getAll()
    }

    fun getLocationsByLocalRouteId(routeId: Int?): List<Location> {
        return database.locationDao().getAllByLocalRouteId(routeId)
    }

    fun deleteLocations(locations: List<Location>) {
        database.locationDao().delete(locations)
    }
}