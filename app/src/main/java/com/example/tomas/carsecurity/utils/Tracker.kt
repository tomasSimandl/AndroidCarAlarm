package com.example.tomas.carsecurity.utils

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjString
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.UtilsContext
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.storage.Storage
import com.example.tomas.carsecurity.storage.entity.Route
import java.util.*
import com.example.tomas.carsecurity.storage.entity.Location as DbLocation

class Tracker(private val context: MyContext, private val utilsHelper: UtilsHelper) : GeneralUtil(utilsHelper), SharedPreferences.OnSharedPreferenceChangeListener {

    private val tag = "utils.Tracker"

    private var lastLocation: Location? = null
    private var isEnabled = false
    private var actualRoute: Route? = null

    //private lateinit var timer: Timer

    override val thisUtilEnum: UtilsEnum = UtilsEnum.Tracker

    companion object Check : CheckObjString {
        override fun check(context: Context, skipAllow: Boolean): String {

            if(!skipAllow && !UtilsContext(context).isTrackerAllowed){
                return context.getString(R.string.error_tracker_disabled)
            }

            val locationCheck = LocationProvider.check(context)

            return when (locationCheck) {
                CheckCodes.hardwareNotSupported -> context.getString(R.string.error_tracker_location_not_supported)
                CheckCodes.permissionDenied -> context.getString(R.string.error_tracker_location_not_permitted)
                CheckCodes.notAllowed -> context.getString(R.string.error_tracker_location_not_allowed)
                CheckCodes.invalidParameters -> context.getString(R.string.error_tracker_location_invalid_params)
                else -> {
                    "" // TODO check for internet provider
                }
            }
        }
    }

    override fun canEnable(): Boolean {
        return check(context.appContext, false).isEmpty()
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        val task = Runnable {
            when (key) {
                // todo restart timer
            }
        }
        utilsHelper.runOnUtilThread(task)
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

        if (location.distanceTo(lastLocation) > context.utilsContext.ignoreDistance) {
            lastLocation = location
            val dbLocation = DbLocation(location, actualRoute?.uid)
            utilsHelper.communicationManager.sendLocation(dbLocation, isAlarm = false, cache = true)

        } else if (location.time - lastLocation!!.time > context.utilsContext.timeout) {
            Log.d(tag, "Time not moving time interval passed. Tracker will be stopped.")
            disable()
        }
    }

    override fun enable() {
        assert(Thread.currentThread().name == "UtilsThread")
        if (!isEnabled && canRun()){
            isEnabled = true
            lastLocation = null
            utilsHelper.registerObserver(ObservableEnum.LocationProvider, this)

            actualRoute = Route(carId = 1) // TODO (Use real car id)
            actualRoute!!.uid = Storage.getInstance(context.appContext).routeService.saveRoute(actualRoute!!).toInt()

            setChanged()
            notifyObservers(true)

            context.utilsContext.registerOnPreferenceChanged(this)

            Log.d(tag, "Tracker system is enabled.")
        }

        utilsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, true)
    }

    override fun disable(force: Boolean) {
        assert(Thread.currentThread().name == "UtilsThread")
        if(isEnabled) {
            isEnabled = false
            utilsHelper.unregisterAllObservables(this)

            context.utilsContext.unregisterOnPreferenceChanged(this)

            if(actualRoute != null) {
                Storage.getInstance(context.appContext).routeService.finishRoute(actualRoute!!)
                actualRoute = null
            }

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

        val msg = check(context.appContext, false)

        return if (msg.isBlank()) {
            true
        } else {
            setChanged()
            notifyObservers(msg)
            false
        }
    }
}