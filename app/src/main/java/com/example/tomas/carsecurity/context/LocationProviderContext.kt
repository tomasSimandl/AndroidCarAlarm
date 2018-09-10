package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.google.android.gms.location.LocationRequest

/**
 * Context contains data which are used in [LocationProvider] class and they are stored in
 * shared preferences or in resources.
 */
class LocationProviderContext(private val sharedPreferences: SharedPreferences, private val context: Context) {

    /** Contains default interval for location updates which is taken from resources. */
    private val defUpdateInterval :Int = context.resources.getInteger(R.integer.default_location_update_interval)

    /** Contains default maximal interval for location updates which is taken from resources. */
    private val defMaxUpdateInterval :Int = context.resources.getInteger(R.integer.default_max_location_update_interval)

    /**
     * Contains default priority of accuracy used when location is requested.
     * Value is taken from resources and it is [LocationRequest] priority constant.
     */
    private val defAccuracyPriority :String = context.resources.getString(R.string.default_location_accuracy_priority)


    /** Returns interval for location updates. Value is taken from shared preferences or it is used default value. */
    val updateInterval
        get() = sharedPreferences.getInt(context.getString(R.string.key_sensor_location_update_interval), defUpdateInterval)

    /** Returns maximal interval for location updates. Value is taken from shared preferences or it is used default value. */
    val maxUpdateInterval
        get() = sharedPreferences.getInt(context.getString(R.string.key_sensor_location_max_update_interval), defMaxUpdateInterval)

    /** Returns priority of accuracy requests. Value is taken from shared preferences or it is used default value. */
    val accuracyPriority: Int
        get() = Integer.valueOf(sharedPreferences.getString(context.getString(R.string.key_sensor_location_accuracy_priority), defAccuracyPriority))
}


