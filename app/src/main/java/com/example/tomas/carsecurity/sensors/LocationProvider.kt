package com.example.tomas.carsecurity.sensors

import android.Manifest
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.util.Log
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.context.LocationProviderContext
import com.example.tomas.carsecurity.context.MyContext
import com.google.android.gms.location.*

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
                setChanged()
                notifyObservers(locationResult.lastLocation)
            }
        }
    }

    override fun enable(){

        val locationRequest = LocationRequest().apply {
            interval = locationProviderContext.updateInterval
            fastestInterval = locationProviderContext.maxUpdateInterval
            priority = locationProviderContext.accuracyPriority

        }

        if (ContextCompat.checkSelfPermission(context.appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) { // TODO better permission check
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            enabled = true
            Log.d(tag, "Provider is enabled")
        } else {
            Log.d(tag, "Provider is not permitted by user.")
        }
    }

    override fun disable(){
        fusedLocationClient.removeLocationUpdates(locationCallback)
        enabled = false
        Log.d(tag, "Provider is disabled.")
    }

    override fun isEnable(): Boolean {
        return enabled
    }
}