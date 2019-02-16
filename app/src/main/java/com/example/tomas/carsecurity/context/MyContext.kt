package com.example.tomas.carsecurity.context

import android.content.Context
import android.os.Looper
import com.example.tomas.carsecurity.storage.Storage

class MyContext(val appContext: Context, val mainServiceThreadLooper: Looper) {

    val sensorContext = SensorContext(appContext)
    val utilsContext = UtilsContext(appContext)
    val communicationContext = CommunicationContext(appContext)

    fun destroy(){
        Storage.destroy()
    }
}