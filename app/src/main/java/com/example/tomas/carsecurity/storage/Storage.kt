package com.example.tomas.carsecurity.storage

import android.arch.persistence.room.Room
import android.content.Context
import com.example.tomas.carsecurity.storage.service.LocationService
import com.example.tomas.carsecurity.storage.service.MessageService
import com.example.tomas.carsecurity.storage.service.RouteService

class Storage private constructor(appContext: Context) {

    private lateinit var database: AppDatabase

    lateinit var locationService: LocationService
    lateinit var messageService: MessageService
    lateinit var routeService: RouteService

    // all running on main thread

    companion object {

        private var instance: Storage? = null

        fun getInstance(appContext: Context): Storage {
            // Initialize singleton
            if (instance == null) {
                instance = Storage(appContext)
            }

            // reopen database if it is not initialized or closed
            if (!instance!!::database.isInitialized || !instance!!.database.isOpen) {

                instance!!.database = Room
                        .databaseBuilder(appContext, AppDatabase::class.java, "CarSecurityDB")
                        .fallbackToDestructiveMigration()
                        .build()

                instance!!.locationService = LocationService(instance!!.database)
                instance!!.messageService = MessageService(instance!!.database)
                instance!!.routeService = RouteService(instance!!.database)
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
}