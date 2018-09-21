package com.example.tomas.carsecurity.sensors

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjByte
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.SensorContext
import com.google.android.gms.location.*

class LocationProvider(private val context: MyContext) : GeneralObservable(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val tag = "sensors.Location"

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context.appContext)

    private val locationCallback: LocationCallback
    private var enabled = false

    init{
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                Log.d(tag, """Update - Thread: ${Thread.currentThread().name}""")
                setChanged()
                notifyObservers(locationResult.lastLocation)
            }
        }
    }

    companion object Check: CheckObjByte {
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

    override fun canEnable(): Boolean {
        return check(context.appContext) == CheckCodes.success
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        when (key) {
            context.appContext.getString(R.string.key_sensor_location_update_interval),
            context.appContext.getString(R.string.key_sensor_location_accuracy_priority),
            context.appContext.getString(R.string.key_sensor_location_max_update_interval) -> {

                fusedLocationClient.removeLocationUpdates(locationCallback)

                try {
                    fusedLocationClient.requestLocationUpdates(getLoactionRequest(), locationCallback, context.mainServiceThreadLooper)
                } catch (e: SecurityException) {
                    disable()
                }
            }
        }
    }

    override fun enable() {
        if (!enabled && check(context.appContext) == CheckCodes.success) {
            try {
                fusedLocationClient.requestLocationUpdates(getLoactionRequest(), locationCallback, context.mainServiceThreadLooper)
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

    private fun getLoactionRequest(): LocationRequest {
        return LocationRequest().apply {
            interval = context.sensorContext.updateInterval.toLong()
            fastestInterval = context.sensorContext.maxUpdateInterval.toLong()
            priority = context.sensorContext.accuracyPriority
        }
    }

    override fun disable(){
        if (enabled) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            enabled = false
            context.sensorContext.unregisterOnPreferenceChanged(this)
            Log.d(tag, "Provider is disabled.")
        }
    }

    override fun isEnable(): Boolean {
        return enabled
    }
}