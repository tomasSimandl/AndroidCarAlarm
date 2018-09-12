package com.example.tomas.carsecurity.context

import android.content.Context
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.sensors.MoveDetector
import com.example.tomas.carsecurity.sensors.SoundDetector

class SensorContext(appContext: Context) : BaseContext(appContext) {


    // ======================================== MOVE SENSOR ========================================

    /** Returns if [MoveDetector] can be in default used. Value is taken from shared preferences or it is used default value. */
    val isMoveAllowed
        get() = getBoolean(R.string.key_sensor_move_is_allowed, R.bool.default_util_is_move_detector_available)

    /** Returns accelerometer sensitivity. Value is taken from shared preferences or it is used default value. */
    val sensitivity: Float
        get() = getInt(R.string.key_sensor_move_sensitivity, R.integer.default_move_sensor_sensitivity) / 100F

    /** Contains default dimension of data from accelerometer sensor. Value is taken from resources. */
    val dimensions: Int = appContext.resources.getInteger(R.integer.default_move_sensor_dimensions)


    // ======================================== SOUND SENSOR =======================================

    /** Returns if [SoundDetector] can be in default used. Value is taken from shared preferences or it is used default value. */
    val isSoundAllowed
        get() = getBoolean(R.string.key_sensor_sound_is_allowed, R.bool.default_util_is_sound_detector_available)

    /** Return maximal allowed amplitude before alarm is triggered. Value is from shared preferences or it is default value. */
    val maxAmplitude
        get() = getInt(R.string.key_sensor_sound_max_ok_amplitude, R.integer.default_max_ok_amplitude)

    /** Return measure interval from shared preferences or use default value */
    val measureInterval: Int
        get() = getInt(R.string.key_sensor_sound_interval, R.integer.default_sound_detector_interval)


    // ====================================== LOCATION SENSOR ======================================

    /** Returns if [LocationProvider] can be in default used. Value is taken from shared preferences or it is used default value. */
    val isLocationAllowed
        get() = getBoolean(R.string.key_sensor_location_is_allowed, R.bool.default_util_is_location_provider_available)

    /** Returns interval for location updates. Value is taken from shared preferences or it is used default value. */
    val updateInterval
        get() = getInt(R.string.key_sensor_location_update_interval, R.integer.default_location_update_interval)

    /** Returns maximal interval for location updates. Value is taken from shared preferences or it is used default value. */
    val maxUpdateInterval
        get() = getInt(R.string.key_sensor_location_max_update_interval, R.integer.default_max_location_update_interval)

    /** Returns priority of accuracy requests. Value is taken from shared preferences or it is used default value. */
    val accuracyPriority: Int
        get() = Integer.valueOf(getString(R.string.key_sensor_location_accuracy_priority, R.string.default_location_accuracy_priority))
}