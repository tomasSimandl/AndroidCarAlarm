package com.example.tomas.carsecurity.utils

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import com.example.tomas.carsecurity.storage.entity.Location as DbLocation
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.CheckObjString
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.TrackerContext
import com.example.tomas.carsecurity.sensors.LocationProvider
import java.util.*

class Tracker(private val context: MyContext, private val utilsHelper: UtilsHelper) : GeneralUtil(utilsHelper) {

    private val tag = "utils.Tracker"

    private val trackerContext = TrackerContext(context.sharedPreferences, context.appContext)
    private var lastLocation: Location? = null
    private var isEnabled = false

    private lateinit var timer: Timer

    override val thisUtilEnum: UtilsEnum = UtilsEnum.Tracker

    companion object Check : CheckObjString {
        override fun check(context: Context, sharedPreferences: SharedPreferences): String {
            val locationCheck = LocationProvider.check(context , sharedPreferences)

            return when (locationCheck) {  // TODO use strings from resources
                CheckCodes.hardwareNotSupported -> "Tracker needs to get device location for creating of log book but this device not support location access."
                CheckCodes.permissionDenied -> "Tracker needs to get device location for creating of log book but application is not permitted to get device location."
                CheckCodes.notAllowed -> "Tracker needs to get device location for creating of log book but sensor is disabled by user."
                else -> {
                    "" // TODO check for internet provider
                }
            }
        }
    }

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

        if (location.distanceTo(lastLocation) > trackerContext.ignoreDistance) {
            lastLocation = location
            context.database.locationDao().insert(DbLocation(location))

        } else if (location.time - lastLocation!!.time > trackerContext.timeout) {
            Log.d(tag, "Time not moving time interval passed. Tracker will be stopped.")
            disable()
        }
    }


    private fun initializeTimer(){
        val timerTask = object : TimerTask() {
            override fun run() {
                Log.d(tag, """Update - Thread: ${Thread.currentThread().name}""")
                synchronize()
            }
        }

        timer = Timer("TrackerTimer")
        timer.schedule( timerTask, 30000, 30000) // TODO const
    }

    private fun synchronize() { // TODO move all method to synchronizeManager? and run all in separate thread
        var run: Boolean
        do {
            if(!context.database.isOpen) return

            val locations = context.database.locationDao().getAll(10) // TODO const
            run = locations.isNotEmpty()

            if(run) {
                println(locations) // TODO send to server
                context.database.locationDao().delete(locations) // TODO if send success
            }
        } while (run)
    }

    override fun enable() {
        if (!isEnabled && canRun()){
            isEnabled = true
            lastLocation = null
            utilsHelper.registerObserver(ObservableEnum.LocationProvider, this)

            initializeTimer()

            setChanged()
            notifyObservers(true)

            Log.d(tag, "Tracker system is enabled.")
        }

        utilsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, true)
    }

    override fun disable() {
        if(isEnabled) {
            isEnabled = false
            utilsHelper.unregisterAllObservables(this)

            if(::timer.isInitialized) timer.cancel()
            synchronize()

            setChanged()
            notifyObservers(false)

            Log.d(tag, "Tracker system is disabled.")
        }

        utilsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, false)
    }

    override fun isEnabled(): Boolean {
        return isEnabled
    }

    private fun canRun(): Boolean {

        val msg = check(context.appContext , context.sharedPreferences)

        return if (msg.isBlank()) {
            true
        } else {
            setChanged()
            notifyObservers(msg)
            false
        }
    }
}