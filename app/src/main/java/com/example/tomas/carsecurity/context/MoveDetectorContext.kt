package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import android.util.TypedValue
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.sensors.MoveDetector

/**
 * Context contains data which are used in [MoveDetector] class and they are stored in
 * shared preferences or in resources.
 */
class MoveDetectorContext(private val sharedPreferences: SharedPreferences, private val context: Context) {

    /** Contains default value for accelerometer sensitivity which is taken from resources. */
    private val defSensitivity :Int = context.resources.getInteger(R.integer.default_move_sensor_sensitivity)
    /** Returns accelerometer sensitivity. Value is taken from shared preferences or it is used default value. */
    val sensitivity: Float
        get() = sharedPreferences.getInt(context.getString(R.string.key_move_sensor_sensitivity), defSensitivity) / 100F

    /** Contains default dimension of data from accelerometer sensor. Value is taken from resources. */
    val dimensions :Int = context.resources.getInteger(R.integer.default_move_sensor_dimensions)

}


