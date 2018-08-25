package com.example.tomas.carsecurity.utils

import android.location.Location
import android.util.Log
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.LocationProvider
import java.util.*

class Tracker(private val context: MyContext, private val utilsHelper: UtilsHelper) : GeneralUtil(context, utilsHelper) {

    private val tag = "utils.Tracker"
    private var enabled = false

    override fun action(observable: Observable, args: Any?) {
        if(!enabled) return

        when(observable){
            is LocationProvider -> onLocationUpdate(args as Location)
            else -> Log.w(tag, """Unsupported observable: $observable""")
        }
    }

    private fun onLocationUpdate(location: Location) {
        Log.d(tag, """Location update $location""")
        // TODO store location
        // TODO send location to server
    }

    override fun enable(): Boolean{
        enabled = true
        utilsHelper.registerObserver(ObservableEnum.LocationProvider, this)

        return true // tracker status
    }

    override fun disable(): Boolean {
        enabled = false
        utilsHelper.unregisterAllObservables(this)

        return false // tracker status
    }

    override fun isEnabled(): Boolean {
        return enabled
    }
}