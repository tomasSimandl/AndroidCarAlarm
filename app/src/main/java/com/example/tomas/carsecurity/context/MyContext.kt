package com.example.tomas.carsecurity.context

import android.arch.persistence.room.Room
import android.content.Context
import android.os.Looper
import com.example.tomas.carsecurity.storage.AppDatabase

class MyContext(val appContext: Context, val mainServiceThreadLooper: Looper) {

    val database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()

    val sensorContext = SensorContext(appContext)
    val utilsContext = UtilsContext(appContext)
    val communicationContext = CommunicationContext(appContext)

    fun destroy(){
        database.close()
    }
}