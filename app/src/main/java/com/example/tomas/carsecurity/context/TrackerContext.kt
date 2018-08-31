package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.utils.Tracker

/**
 * Context contains data which are used in [Tracker] class and they are stored in
 * shared preferences or in resources.
 */
class TrackerContext(private val sharedPreferences: SharedPreferences, private val context: Context) {

    /** Contains default new location ignore distance in meters. Value is taken from resources. */
    private val defIgnoreDistance = context.resources.getInteger(R.integer.default_tracker_ignore_distance)
    /** Contains default not moving timeout in milliseconds. Value is taken from resources. */
    private val defTimeout = context.resources.getInteger(R.integer.default_tracker_timeout)

    /** Returns new location ignore distance in meters. Value is taken from shared preferences or it is used default value. */
    val ignoreDistance
        get() = sharedPreferences.getInt(context.getString(R.string.key_tracker_ignore_distance), defIgnoreDistance)

    /** Returns not moving timeout in milliseconds. Value is taken from shared preferences or it is used default value. */
    val timeout
        get() = sharedPreferences.getInt(context.getString(R.string.key_tracker_timeout), defTimeout)
}


