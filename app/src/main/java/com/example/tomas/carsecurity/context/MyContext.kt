package com.example.tomas.carsecurity.context

import android.arch.persistence.room.Room
import android.content.Context
import android.os.Looper
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.storage.AppDatabase
import java.util.*

class MyContext(val appContext: Context, val mainServiceThreadLooper: Looper) : Observable() {

    val database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()

    val sensorContext = SensorContext(appContext)
    val utilsContext = UtilsContext(appContext)

    fun updateContext(){
        setChanged()
        notifyObservers()
    }

    fun destroy(){
        database.close()
    }
}