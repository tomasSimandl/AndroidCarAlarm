package com.example.tomas.carsecurity.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.example.tomas.carsecurity.storage.dao.LocationDao
import com.example.tomas.carsecurity.storage.entity.Location

@Database(entities = [Location::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun locationDao(): LocationDao
}