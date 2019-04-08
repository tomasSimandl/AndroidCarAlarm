package com.example.tomas.carsecurity.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.example.tomas.carsecurity.storage.dao.LocationDao
import com.example.tomas.carsecurity.storage.dao.MessageDao
import com.example.tomas.carsecurity.storage.dao.RouteDao
import com.example.tomas.carsecurity.storage.dao.UserDao
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.storage.entity.Message
import com.example.tomas.carsecurity.storage.entity.Route
import com.example.tomas.carsecurity.storage.entity.User

/**
 * Representation of local Room database
 */
@Database(entities = [Location::class, Message::class, Route::class, User::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /** DAO for access location table */
    abstract fun locationDao(): LocationDao

    /** DAO for access message table */
    abstract fun messageDao(): MessageDao

    /** DAO for access route table */
    abstract fun routeDao(): RouteDao

    /** DAO for access user table */
    abstract fun userDao(): UserDao
}