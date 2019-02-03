package com.example.tomas.carsecurity.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.example.tomas.carsecurity.storage.dao.LocationDao
import com.example.tomas.carsecurity.storage.dao.MessageDao
import com.example.tomas.carsecurity.storage.dao.RouteDao
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.storage.entity.Message
import com.example.tomas.carsecurity.storage.entity.Route

@Database(entities = [Location::class, Message::class, Route::class], version = 2)
abstract class AppDatabase: RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun messageDao(): MessageDao
    abstract fun routeDao(): RouteDao
}