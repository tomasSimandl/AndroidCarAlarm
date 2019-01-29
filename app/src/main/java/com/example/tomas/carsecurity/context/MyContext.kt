package com.example.tomas.carsecurity.context

import android.content.Context
import android.os.Looper
import com.example.tomas.carsecurity.storage.StorageService

class MyContext(val appContext: Context, val mainServiceThreadLooper: Looper) {

    val storageService = StorageService(appContext)

    val sensorContext = SensorContext(appContext)
    val utilsContext = UtilsContext(appContext)
    val communicationContext = CommunicationContext(appContext)

    fun destroy(){
        storageService.close()
    }
}