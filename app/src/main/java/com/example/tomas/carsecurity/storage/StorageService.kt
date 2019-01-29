package com.example.tomas.carsecurity.storage

import android.arch.persistence.room.Room
import android.content.Context
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.storage.entity.Message
import com.example.tomas.carsecurity.storage.entity.Route

class StorageService(appContext: Context) {

    private val database = Room.databaseBuilder(appContext, AppDatabase::class.java, "CarSecurityDB").build()

    // all running on main thread

    fun close(){
        database.close()
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

    fun saveLocation(location: Location) {
        database.locationDao().insert(location)
    }

    fun getLocations(): List<Location> {
        return database.locationDao().getAll()
    }

    fun deleteLocations(locations: List<Location>) {
        database.locationDao().delete(locations)
    }


    fun saveRoute(route: Route) {
        database.routeDao().insert(route)
    }

    fun finishRoute(route: Route) {
        route.finished = true
        database.routeDao().update(route)
    }
}