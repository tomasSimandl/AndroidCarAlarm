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

@Database(entities = [Location::class, Message::class, Route::class, User::class], version = 3)
abstract class AppDatabase: RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun messageDao(): MessageDao
    abstract fun routeDao(): RouteDao
    abstract fun userDao(): UserDao
}