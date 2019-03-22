package com.example.tomas.carsecurity.sensors

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjByte
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.SensorContext
import com.google.android.gms.location.*

/**
 * Class is used for getting data from location sensor and promote this data via Observer design pattern.
 *
 * @param context used for access values in shared preferences.
 */
class LocationProvider(private val context: MyContext) : GeneralObservable(), SharedPreferences.OnSharedPreferenceChangeListener {

    /**Logger tag */
    private val tag = "sensors.Location"

    /** Location client which enables access to location sensor */
    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context.appContext)

    /** Callback used for getting data from location sensor */
    private val locationCallback: LocationCallback
    /** Indication if observation of location sensor is enabled. */
    private var enabled = false

    /**
     * Initialization of locationCallback which notify all observers when value in location sensor has changed.
     */
    init {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                Log.d(tag, """Update - Thread: ${Thread.currentThread().name}""")
                setChanged()
                notifyObservers(locationResult.lastLocation)
            }
        }
    }

    /**
     * Object is used for static calling of check method.
     */
    companion object Check : CheckObjByte {
        /**
         * Method checks if observation of location sensor can be enabled.
         * @param context is application context.
         * @return [CheckCodes] value indicates result of check.
         */
        override fun check(context: Context): Byte {
            return if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)) {
                CheckCodes.hardwareNotSupported
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                CheckCodes.permissionDenied
            } else if (!SensorContext(context).isLocationAllowed) {
                CheckCodes.notAllowed
            } else {
                CheckCodes.success
            }
        }
    }

    /**
     * Method check if location sensor observation can be enabled.
     *
     * @return true if location sensor observation can be enabled, false otherwise.
     */
    override fun canEnable(): Boolean {
        return check(context.appContext) == CheckCodes.success
    }

    /**
     * Method is automatically called when some value in sharedPreferenceis is changed.  Method reacts only on change
     * of values witch configure location sensor. On their change location sensor is reinitialized.
     *
     * @param p0 is [SharedPreferences] where changes was happen.
     * @param key is key of witch value was changed.
     */
    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        val task = Runnable {
            when (key) {
                context.appContext.getString(R.string.key_sensor_location_update_interval),
                context.appContext.getString(R.string.key_sensor_location_accuracy_priority),
                context.appContext.getString(R.string.key_sensor_location_max_update_interval) -> {

                    fusedLocationClient.removeLocationUpdates(locationCallback)

                    try {
                        fusedLocationClient.requestLocationUpdates(getLocationRequest(), locationCallback, context.mainServiceThreadLooper)
                    } catch (e: SecurityException) {
                        disable()
                    }
                }
            }
        }
        (context.mainServiceThreadLooper.thread as WorkerThread).postTask(task)
    }

    /**
     * Method enables observation of location sensor.
     */
    override fun enable() {
        if (!enabled && check(context.appContext) == CheckCodes.success) {
            try {
                fusedLocationClient.requestLocationUpdates(getLocationRequest(), locationCallback, context.mainServiceThreadLooper)
                enabled = true
                context.sensorContext.registerOnPreferenceChanged(this)
                Log.d(tag, "Provider is enabled")
            } catch (e: SecurityException) {
                Log.d(tag, "request location - permission denied.")
            }
        } else {
            Log.d(tag, "Provider is not permitted by user.")
        }
    }

    /**
     * Create and return configuration for location sensor. Configuration is taken from sharedPreferences.
     *
     * @return configuration for location sensor.
     */
    private fun getLocationRequest(): LocationRequest {
        return LocationRequest().apply {
            interval = context.sensorContext.updateInterval.toLong()
            fastestInterval = context.sensorContext.maxUpdateInterval.toLong()
            priority = context.sensorContext.accuracyPriority
        }
    }

    /**
     * Disable observation of location sensor.
     */
    override fun disable() {
        if (enabled) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            enabled = false
            context.sensorContext.unregisterOnPreferenceChanged(this)
            Log.d(tag, "Provider is disabled.")
        }
    }

    /**
     * Indication if Location sensor is activated and observe by this class.
     *
     * @return true if location sensor is activated and observer.
     */
    override fun isEnable(): Boolean {
        return enabled
    }
}