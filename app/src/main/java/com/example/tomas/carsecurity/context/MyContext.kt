package com.example.tomas.carsecurity.context

import android.content.Context
import android.os.Looper
import com.example.tomas.carsecurity.storage.Storage

/**
 * Represents context used mainly for access values from shared preferences.
 *
 * @param appContext is application context which will be shared across whole application.
 * @param mainServiceThreadLooper is looper of MainServiceThread used for running tasks on this thread.
 */
class MyContext(val appContext: Context, val mainServiceThreadLooper: Looper) {

    /** Specialized context used in sensor package. */
    val sensorContext = SensorContext(appContext)
    /** Specialized context used in tools package. */
    val toolsContext = ToolsContext(appContext)
    /** Specialized context used in communication package */
    val communicationContext = CommunicationContext(appContext)

    /**
     * This method should be called on application exit and is used for closing connection to database.
     */
    fun destroy() {
        Storage.destroy()
    }
}