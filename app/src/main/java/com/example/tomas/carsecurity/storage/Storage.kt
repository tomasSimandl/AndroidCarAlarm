package com.example.tomas.carsecurity.storage

import android.arch.persistence.room.Room
import android.content.Context
import com.example.tomas.carsecurity.storage.service.LocationService
import com.example.tomas.carsecurity.storage.service.MessageService
import com.example.tomas.carsecurity.storage.service.RouteService
import com.example.tomas.carsecurity.storage.service.UserService

/**
 * Class used for access Room database. Class is created by Singleton design pattern.
 */
class Storage private constructor() {

    /** Instance of local Room database. */
    private lateinit var database: AppDatabase

    /** Service for access data in location table */
    lateinit var locationService: LocationService
    /** Service for access data in message table */
    lateinit var messageService: MessageService
    /** Service for access data in route table */
    lateinit var routeService: RouteService
    /** Service for access data in user table */
    lateinit var userService: UserService


    /**
     * Object for static access to getInstance method which is used for realization of Singleton.
     */
    companion object {

        /** Instance [Storage] which is unique in whole application lifetime */
        private var instance: Storage? = null

        /**
         * Initialize Room database and return its instance. Instance is created only at first call after that is
         * always returned the same instance.
         *
         * @param appContext is application context
         * @return instance of [Storage]
         */
        fun getInstance(appContext: Context): Storage {
            // Initialize singleton
            if (instance == null) {
                instance = Storage()
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
                instance!!.userService = UserService(instance!!.database)
            }

            return instance!!
        }

        /**
         * Method close open Room database. Method should be called at the end of using of database.
         */
        fun destroy() {
            instance?.close()
        }
    }

    /**
     * Method wipe all data from all tables.
     */
    fun clearAllTables() {
        database.clearAllTables()
    }

    /**
     * Method close connection to database if it is open.
     */
    fun close() {
        if (database.isOpen) {
            database.close()
        }
    }
}