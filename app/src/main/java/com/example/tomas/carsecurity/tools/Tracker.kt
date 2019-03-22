package com.example.tomas.carsecurity.tools

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjString
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.network.NetworkProvider
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.ToolsContext
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.storage.Storage
import com.example.tomas.carsecurity.storage.entity.Route
import java.util.*
import com.example.tomas.carsecurity.storage.entity.Location as DbLocation

class Tracker(private val context: MyContext, private val toolsHelper: ToolsHelper) : GeneralTool(toolsHelper), SharedPreferences.OnSharedPreferenceChangeListener {

    private val tag = "tools.Tracker"

    private var lastLocation: Location? = null
    private var isEnabled = false
    private var actualRoute: Route? = null

    //private lateinit var timer: Timer

    override val thisUtilEnum: ToolsEnum = ToolsEnum.Tracker

    companion object Check : CheckObjString {
        override fun check(context: Context, skipAllow: Boolean): String {

            if(!skipAllow && !ToolsContext(context).isTrackerAllowed){
                return context.getString(R.string.error_tracker_disabled)
            }

            val locationCheck = LocationProvider.check(context)
            when (locationCheck) {
                CheckCodes.hardwareNotSupported -> return context.getString(R.string.error_tracker_location_not_supported)
                CheckCodes.permissionDenied -> return context.getString(R.string.error_tracker_location_not_permitted)
                CheckCodes.notAllowed -> return context.getString(R.string.error_tracker_location_not_allowed)
                CheckCodes.invalidParameters -> return context.getString(R.string.error_tracker_location_invalid_params)
            }

            val networkCheck = NetworkProvider.check(context)
            when (networkCheck) {
                CheckCodes.hardwareNotSupported -> return context.getString(R.string.error_tracker_network_not_supported)
                CheckCodes.permissionDenied -> return context.getString(R.string.error_tracker_network_not_permitted)
                CheckCodes.notAllowed -> return context.getString(R.string.error_tracker_network_not_allowed)
                CheckCodes.invalidParameters -> return context.getString(R.string.error_tracker_network_invalid_params)
            }
            return ""
        }
    }

    override fun canEnable(): Boolean {
        return check(context.appContext, false).isEmpty() && context.communicationContext.isLogin
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        val task = Runnable {
            when (key) {
                context.appContext.getString(R.string.key_communication_network_is_user_login) -> {
                    if(isEnabled && !context.communicationContext.isLogin) {
                        disable()
                    }
                }
            }
        }
        toolsHelper.runOnUtilThread(task)
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
            val dbLocation = DbLocation(location, actualRoute?.uid)
            toolsHelper.communicationManager.sendLocation(dbLocation, isAlarm = false, cache = true)
            return
        }

        if (location.distanceTo(lastLocation) > context.toolsContext.ignoreDistance) {
            val dbLocation = DbLocation(location, actualRoute?.uid, location.distanceTo(lastLocation))
            lastLocation = location
            toolsHelper.communicationManager.sendLocation(dbLocation, isAlarm = false, cache = true)

        } else if (location.time - lastLocation!!.time > context.toolsContext.timeout) {
            Log.d(tag, "Time not moving time interval passed. Tracker will be stopped.")
            disable()
        }
    }

    override fun enable() {
        assert(Thread.currentThread().name == "UtilsThread")
        if (!isEnabled && canRun()){

            val storage  = Storage.getInstance(context.appContext)
            val user = storage.userService.getUser()
            if(user == null){
                Log.d(tag, "Can not enable tracker. User is not logged in.")
                setChanged()
                notifyObservers(context.appContext.getString(R.string.error_tracker_not_log_in))
                return
            }

            isEnabled = true
            lastLocation = null
            toolsHelper.registerObserver(ObservableEnum.LocationProvider, this)

            actualRoute = Route(carId = user.carId)
            actualRoute!!.uid = storage.routeService.saveRoute(actualRoute!!).toInt()

            setChanged()
            notifyObservers(true)

            context.toolsContext.registerOnPreferenceChanged(this)

            Log.d(tag, "Tracker system is enabled.")
            toolsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, true)
        }

    }

    override fun disable(force: Boolean) {
        assert(Thread.currentThread().name == "UtilsThread")
        if(isEnabled) {
            isEnabled = false
            toolsHelper.unregisterAllObservables(this)

            context.toolsContext.unregisterOnPreferenceChanged(this)

            if(actualRoute != null) {
                actualRoute = null
            }

            setChanged()
            notifyObservers(false)

            Log.d(tag, "Tracker system is disabled.")
            toolsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, false)
        }

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