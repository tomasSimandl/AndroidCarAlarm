package com.example.tomas.carsecurity.utils

import android.location.Location
import android.util.Log
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.LocationProvider
import java.util.*

class Tracker(context: MyContext, private val utilsHelper: UtilsHelper) : GeneralUtil(utilsHelper) {

    private val tag = "utils.Tracker"

    private var lastLocation: Location? = null
    private var isEnabled = false

    override val thisUtilEnum: UtilsEnum = UtilsEnum.Tracker

    override fun action(observable: Observable, args: Any?) {
        if (!isEnabled) return

        when (observable) {
            is LocationProvider -> onLocationUpdate(args as Location)
            else -> Log.w(tag, """Unsupported observable: $observable""")
        }
    }

    private fun onLocationUpdate(location: Location) {
        Log.d(tag, """Location update $location""")

        if(lastLocation == null) {
            lastLocation = location
            return
        }

        if (location.distanceTo(lastLocation) > 10) { // TODO const
            lastLocation = location
            // TODO store location
            // TODO send location to server

        } else if (location.time - lastLocation!!.time > 6000) { // 1000 * 60 * 10 - 10 minutes // TODO const
            Log.d(tag, "Time not moving time interval passed. Tracker will be stopped.")
            disable()
        }
    }

    override fun enable(): Boolean {
        if (!isEnabled){
            isEnabled = true
            lastLocation = null
            utilsHelper.registerObserver(ObservableEnum.LocationProvider, this)

            setChanged()
            notifyObservers(true)

            Log.d(tag, "Tracker system is enabled.")
        }

        utilsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, true)
        return true // tracker status
    }

    override fun disable(): Boolean {
        if(isEnabled) {
            isEnabled = false
            utilsHelper.unregisterAllObservables(this)

            setChanged()
            notifyObservers(false)

            Log.d(tag, "Tracker system is disabled.")

        }

        utilsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, false)
        return false // tracker status
    }

    override fun isEnabled(): Boolean {
        return isEnabled
    }
}