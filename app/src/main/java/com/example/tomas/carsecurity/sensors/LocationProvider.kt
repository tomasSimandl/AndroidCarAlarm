package com.example.tomas.carsecurity.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.*
import java.util.*

class LocationProvider(private val context: Context) : Observable() {

    private var fusedLocationClient: FusedLocationProviderClient

    private val locationCallback: LocationCallback


    init{
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                setChanged()
                notifyObservers(locationResult.lastLocation)
            }
        }
    }

    fun enable(){

        val locationRequest = LocationRequest().apply {
            interval = 3000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    fun disable(){
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }



}