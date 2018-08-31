package com.example.tomas.carsecurity

import android.arch.persistence.room.Room
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.example.tomas.carsecurity.storage.AppDatabase
import com.example.tomas.carsecurity.storage.entity.Location
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var appContext: Context
    private lateinit var database: AppDatabase

    @Before
    fun init() {
        appContext = InstrumentationRegistry.getTargetContext()
        database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        database.clearAllTables()
    }

    @Test
    fun insertAndGetData(){

        val location = Location()
        location.accuracy = 10F
        location.altitude = 123.0
        location.latitude = 92.8283
        location.longitude = 129.8202
        location.speed = 120F
        location.time = 34567890

        database.locationDao().insert(location)

        val list = database.locationDao().getAll()

        assertEquals(1, list.size)
        assertEquals(10F, list[0].accuracy)
        assertTrue(123.0 == list[0].altitude)
        assertTrue(92.8283 == list[0].latitude)
        assertTrue(129.8202 == list[0].longitude)
        assertEquals(120F, list[0].speed)
        assertEquals(34567890, list[0].time)
    }

    @Test
    fun insertAndDeleteAll(){

        var list = database.locationDao().getAll()
        assertEquals(0, list.size)

        database.locationDao().insert(Location())
        database.locationDao().insert(Location())
        database.locationDao().insert(Location())

        list = database.locationDao().getAll()
        assertEquals(3, list.size)

        database.locationDao().deleteAll()

        list = database.locationDao().getAll()
        assertEquals(0, list.size)
    }
}