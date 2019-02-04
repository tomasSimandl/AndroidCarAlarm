package com.example.tomas.carsecurity.storage

import android.arch.persistence.room.Room
import android.content.Context
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.storage.entity.Message
import com.example.tomas.carsecurity.storage.entity.Route

class StorageService private constructor(appContext: Context) {

    private lateinit var database: AppDatabase

    // all running on main thread

    companion object {

        private var instance: StorageService? = null

        fun getInstance(appContext: Context): StorageService {
            // Initialize singleton
            if (instance == null) {
                instance = StorageService(appContext)
            }

            // reopen database if it is not initialized or closed
            if (!instance!!::database.isInitialized || !instance!!.database.isOpen) {
                instance!!.database = Room.databaseBuilder(appContext, AppDatabase::class.java, "CarSecurityDB")
                        .fallbackToDestructiveMigration()
                        .build()
            }

            return instance!!
        }

        fun destroy() {
            instance?.close()
        }
    }

    fun close() {
        if (database.isOpen) {
            database.close()
        }
    }

    fun saveMessage(message: Message) {
        database.messageDao().insert(message)
    }

    fun getMessages(communicatorHash: Int): List<Message> {
        val messages = database.messageDao().getAllByCommunicatorHash(communicatorHash)
        database.messageDao().delete(messages)

        return messages
    }

    fun deleteMessages(communicatorHash: Int) {
        database.messageDao().deleteAllByCommunicatorHash(communicatorHash)
    }

    fun deleteMessage(message: Message) {
        database.messageDao().delete(message)
    }

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


    fun saveRoute(route: Route): Long{
        return database.routeDao().insert(route)
    }

    fun updateRoute(route: Route) {
        database.routeDao().update(route)
    }

    fun getRoutes(): List<Route> {
        return database.routeDao().getAll()
    }

    fun getRoute(routeId: Int): Route {
        return database.routeDao().get(routeId)
    }

    fun deleteRoute(route: Route) {
        database.routeDao().delete(route)
    }

    fun finishRoute(route: Route) {
        route.finished = true
        database.routeDao().update(route)
    }
}