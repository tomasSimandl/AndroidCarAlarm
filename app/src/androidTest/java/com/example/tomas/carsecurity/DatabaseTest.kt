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
import org.junit.After



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

    @After
    fun closeDb() {
        database.close()
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

    @Test
    fun selectLimit(){
        var list = database.locationDao().getAll()
        assertEquals(0, list.size)

        database.locationDao().insert(Location())
        database.locationDao().insert(Location())
        database.locationDao().insert(Location())

        list = database.locationDao().getAll()
        assertEquals(3, list.size)

        list = database.locationDao().getAll(2)
        assertEquals(2, list.size)
    }

    @Test
    fun deleteTwoItems(){
        var list = database.locationDao().getAll()
        assertEquals(0, list.size)

        val l1 = Location()
        val l2 = Location()
        val l3 = Location()
        l1.longitude = 1.0
        l2.longitude = 2.0
        l3.longitude = 3.0

        database.locationDao().insert(l1)
        database.locationDao().insert(l2)
        database.locationDao().insert(l3)

        list = database.locationDao().getAll()
        assertEquals(3, list.size)
        assertTrue(list[0].longitude == 1.0)
        assertTrue(list[1].longitude == 2.0)
        assertTrue(list[2].longitude == 3.0)

        database.locationDao().delete(list[0], list[1])

        list = database.locationDao().getAll(2)
        assertEquals(1, list.size)
        assertTrue(list[0].longitude == 3.0)
    }

    @Test
    fun deleteTwoItemsWithList(){
        var list = database.locationDao().getAll()
        assertEquals(0, list.size)

        val l1 = Location()
        val l2 = Location()
        val l3 = Location()
        l1.longitude = 1.0
        l2.longitude = 2.0
        l3.longitude = 3.0

        database.locationDao().insert(l1)
        database.locationDao().insert(l2)
        database.locationDao().insert(l3)

        list = database.locationDao().getAll()
        assertEquals(3, list.size)
        assertTrue(list[0].longitude == 1.0)
        assertTrue(list[1].longitude == 2.0)
        assertTrue(list[2].longitude == 3.0)

        database.locationDao().delete(listOf(list[1],list[2]))

        list = database.locationDao().getAll(2)
        assertEquals(1, list.size)
        assertTrue(list[0].longitude == 1.0)
    }
}