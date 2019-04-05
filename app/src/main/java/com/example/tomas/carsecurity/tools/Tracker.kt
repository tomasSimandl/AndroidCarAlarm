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

/**
 * Class represents tracker system and all its logic.
 *
 * @param context is my context used mainly for access of shared preferences values.
 * @param toolsHelper used mainly for registration of sensors
 */
class Tracker(private val context: MyContext, private val toolsHelper: ToolsHelper) :
        GeneralTool(toolsHelper),
        SharedPreferences.OnSharedPreferenceChangeListener {

    /** Logger tag */
    private val tag = "tools.Tracker"

    /** Identification of this tool by [ToolsEnum] */
    override val thisToolEnum: ToolsEnum = ToolsEnum.Tracker

    /** Last known location given by location sensor */
    private var lastLocation: Location? = null
    /** Identification if Tracker is enabled */
    private var isEnabled = false
    /** Actual route which is used for storing locations to database. */
    private var actualRoute: Route? = null
    /** Time when [lastLocation] was changed */
    private var lastUpdateTime: Date = Date()
    /** Timer used for checking if timeout for Tracker turn off passed.*/
    private var timeoutTimer: Timer? = null

    /**
     * Object used for static access to [check] method.
     */
    companion object Check : CheckObjString {
        /**
         * Method checks if there is some restriction which prevents of Tracker activation.
         *
         * @param context is application context
         * @param skipAllow indicates if should be check tracker allow attribute set by user.
         * @return Error message when there is some problem or empty string when tracker can be enabled.
         */
        override fun check(context: Context, skipAllow: Boolean): String {

            val toolsContext = ToolsContext(context)

            if (!skipAllow && !toolsContext.isTrackerAllowed) {
                return context.getString(R.string.error_tracker_disabled)
            }

            if (toolsContext.isPowerSaveMode) {
                return context.getString(R.string.error_tracker_in_power_save_mode)
            }

            val locationCheck = LocationProvider.check(context)
            when (locationCheck) {
                CheckCodes.hardwareNotSupported ->
                    return context.getString(R.string.error_tracker_location_not_supported)
                CheckCodes.permissionDenied ->
                    return context.getString(R.string.error_tracker_location_not_permitted)
                CheckCodes.notAllowed ->
                    return context.getString(R.string.error_tracker_location_not_allowed)
                CheckCodes.invalidParameters ->
                    return context.getString(R.string.error_tracker_location_invalid_params)
            }

            val networkCheck = NetworkProvider.check(context)
            when (networkCheck) {
                CheckCodes.hardwareNotSupported ->
                    return context.getString(R.string.error_tracker_network_not_supported)
                CheckCodes.permissionDenied ->
                    return context.getString(R.string.error_tracker_network_not_permitted)
                CheckCodes.notAllowed ->
                    return context.getString(R.string.error_tracker_network_not_allowed)
                CheckCodes.invalidParameters ->
                    return context.getString(R.string.error_tracker_network_invalid_params)
            }
            return ""
        }
    }

    /**
     * Method return if tracker can be enabled with use of [check] method.
     *
     * @return true when tracker can be enabled, false otherwise.
     */
    override fun canEnable(): Boolean {
        return check(context.appContext, false).isEmpty() && context.communicationContext.isLogin
    }

    /**
     * Method is automatically called when any value in sharedPreferences is changed. Method interact only when
     * key of changed value is: communication_network_is_user_login. To detect if user logout.
     * Body of method runs on UtilThread thread.
     *
     * @param p0 is shared preferences storage in which was value changed.
     * @param key of value which was changed.
     */
    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        val task = Runnable {
            when (key) {
                context.appContext.getString(R.string.key_communication_network_is_user_login) -> {
                    if (isEnabled && !context.communicationContext.isLogin) {
                        disable()
                    }
                }
            }
        }
        toolsHelper.runOnUtilThread(task)
    }

    /**
     * This method is called when any observed sensor calls [notifyObservers] method. Only acceptable [Observable]
     * object is [LocationProvider].
     *
     * @param observable is [Observable] object which call [notifyObservers] method.
     * @param args is expected to be [Location]
     */
    override fun action(observable: Observable, args: Any?) {
        if (!isEnabled) return

        when (observable) {
            is LocationProvider -> onLocationUpdate(args as Location)
            else -> Log.w(tag, """Unsupported observable: $observable""")
        }
    }

    /**
     * Method process new location. When distance between actual and last location is smaller
     * than acceptable distance, location is ignored.
     *
     * @param location is new location from [LocationProvider]
     */
    private fun onLocationUpdate(location: Location) {
        Log.d(tag, """Location update $location""")

        if (lastLocation == null) {
            lastLocation = location
            val dbLocation = DbLocation(location, actualRoute?.uid)
            toolsHelper.communicationManager.sendLocation(dbLocation, isAlarm = false, cache = true)
            return
        }

        if (location.distanceTo(lastLocation) > context.toolsContext.ignoreDistance) {
            val dbLocation = DbLocation(location, actualRoute?.uid, location.distanceTo(lastLocation))
            lastLocation = location
            lastUpdateTime = Date()

            toolsHelper.communicationManager.sendLocation(dbLocation, isAlarm = false, cache = true)
            context.toolsContext.actualLength += dbLocation.distance
        }
    }

    /**
     * Method initialize and enable Tracker but only when user is login.
     */
    override fun enable() {
        assert(Thread.currentThread().name == "UtilsThread")
        if (!isEnabled && canRun()) {

            val storage = Storage.getInstance(context.appContext)
            val user = storage.userService.getUser()
            if (user == null) {
                Log.d(tag, "Can not enable tracker. User is not logged in.")
                setChanged()
                notifyObservers(context.appContext.getString(R.string.error_tracker_not_log_in))
                return
            }

            isEnabled = true
            lastLocation = null
            toolsHelper.registerObserver(ObservableEnum.LocationProvider, this)
            context.toolsContext.actualLength = 0f

            actualRoute = Route(carId = user.carId)
            actualRoute!!.uid = storage.routeService.saveRoute(actualRoute!!).toInt()

            lastUpdateTime = Date()
            timeoutTimer = Timer("TimeoutTimer")
            timeoutTimer?.schedule(timeoutCheckTask, context.toolsContext.timeout)

            setChanged()
            notifyObservers(true)

            context.toolsContext.registerOnPreferenceChanged(this)

            Log.d(tag, "Tracker system is enabled.")
            toolsHelper.communicationManager.sendUtilSwitch(thisToolEnum, true)
        }
    }

    /**
     * Method disable tracker and unregister all sensors.
     *
     * @param force is not used in this implementation.
     */
    override fun disable(force: Boolean) {
        assert(Thread.currentThread().name == "UtilsThread")
        if (isEnabled) {
            isEnabled = false
            toolsHelper.unregisterAllObservables(this)

            context.toolsContext.unregisterOnPreferenceChanged(this)
            context.toolsContext.actualLength = -1f

            timeoutTimer?.cancel()
            timeoutTimer = null


            if (actualRoute != null) {
                // Remove route when there is no created position
                val storage = Storage.getInstance(context.appContext)
                val positions = storage.locationService.getLocationsByLocalRouteId(actualRoute!!.uid).size
                if(positions <= 0) {
                    Log.d(tag, "Removing route from database. No positions.")
                    storage.routeService.deleteRoute(actualRoute!!)
                }
                actualRoute = null
            }

            setChanged()
            notifyObservers(false)

            Log.d(tag, "Tracker system is disabled.")
            toolsHelper.communicationManager.sendUtilSwitch(thisToolEnum, false)
        }

    }

    /**
     * Indicates if Tracker is enabled.
     * @return true when tracker is enabled.
     */
    override fun isEnabled(): Boolean {
        return isEnabled
    }

    /**
     * Method use [check] method to check if tracker can be activated.
     * When activation is not possible send error message to all observers of this class.
     *
     * @return true when tracker can be activated, false otherwise.
     */
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

    /**
     * On every get, new [TimerTask] is created. Task is used for checking Tracker disable timeout.
     * On timeout task disable Tracker. Otherwise next task is scheduled.
     */
    private val timeoutCheckTask: TimerTask
        get() = object : TimerTask() {
            override fun run() {
                val timeLeft = context.toolsContext.timeout - (Date().time - lastUpdateTime.time)
                if (timeLeft <= 0) {
                    Log.d(tag, "Not moving time interval passed. Tracker will be stopped.")
                    disable()
                } else {
                    timeoutTimer?.schedule(timeoutCheckTask, timeLeft)
                }
            }
        }
}