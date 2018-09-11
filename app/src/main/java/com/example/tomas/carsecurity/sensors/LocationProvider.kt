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
import com.example.tomas.carsecurity.context.LocationProviderContext
import com.example.tomas.carsecurity.context.MyContext
import com.google.android.gms.location.*
import com.example.tomas.carsecurity.R

class LocationProvider(private val context: MyContext) : GeneralObservable() {

    private val tag = "sensors.Location"

    private val locationProviderContext: LocationProviderContext = LocationProviderContext(context.sharedPreferences, context.appContext)
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
        override fun check(context: Context, sharedPreferences: SharedPreferences): Byte {
            return if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)) {
                CheckCodes.hardwareNotSupported
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                CheckCodes.permissionDenied
            } else if (!sharedPreferences.getBoolean(context.getString(R.string.key_sensor_location_is_allowed), context.resources.getBoolean(R.bool.default_util_is_location_provider_available))) {
                CheckCodes.notAllowed
            } else {
                CheckCodes.success
            }
        }
    }

    override fun enable() {
        if (!enabled && check(context.appContext, context.sharedPreferences) == CheckCodes.success) {

            val locationRequest = LocationRequest().apply {
                interval = locationProviderContext.updateInterval.toLong()
                fastestInterval = locationProviderContext.maxUpdateInterval.toLong()
                priority = locationProviderContext.accuracyPriority
            }

            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainServiceThreadLooper)
                enabled = true
                Log.d(tag, "Provider is enabled")
            } catch (e: SecurityException) {
                Log.d(tag, "request location - permission denied.")
            }
        } else {
            Log.d(tag, "Provider is not permitted by user.")
        }
    }

    override fun disable(){
        if (enabled) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            enabled = false
            Log.d(tag, "Provider is disabled.")
        }
    }

    override fun isEnable(): Boolean {
        return enabled
    }
}