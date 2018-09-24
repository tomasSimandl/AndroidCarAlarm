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
        get() = getBoolean(R.string.key_sensor_move_is_allowed, R.bool.default_sensor_move_is_allowed)

    /** Returns accelerometer sensitivity. Value is taken from shared preferences or it is used default value. */
    val sensitivity: Float
        get() = getInt(R.string.key_sensor_move_sensitivity, R.integer.default_sensor_move_sensitivity) / 100F

    /** Contains default dimension of data from accelerometer sensor. Value is taken from resources. */
    val dimensions: Int = appContext.resources.getInteger(R.integer.default_sensor_move_dimensions)


    // ======================================== SOUND SENSOR =======================================

    /** Returns if [SoundDetector] can be in default used. Value is taken from shared preferences or it is used default value. */
    val isSoundAllowed
        get() = when(mode) {
            Mode.Normal -> getBoolean(R.string.key_sensor_sound_is_allowed, R.bool.default_sensor_sound_is_allowed)
            Mode.PowerSaveMode -> appContext.resources.getBoolean(R.bool.battery_save_mode_sensor_sound_is_allowed)
        }

    /** Return maximal allowed amplitude before alarm is triggered. Value is from shared preferences or it is default value. */
    val maxAmplitude
        get() = getInt(R.string.key_sensor_sound_max_ok_amplitude, R.integer.default_sensor_sound_max_ok_amplitude)

    /** Return measure interval from shared preferences or use default value */
    val measureInterval: Int
        get() = getInt(R.string.key_sensor_sound_interval, R.integer.default_sensor_sound_interval) * 1000


    // ====================================== LOCATION SENSOR ======================================

    /** Returns if [LocationProvider] can be in default used. Value is taken from shared preferences or it is used default value. */
    val isLocationAllowed
        get() = getBoolean(R.string.key_sensor_location_is_allowed, R.bool.default_sensor_location_is_allowed)

    /** Returns interval for location updates. Value is taken from shared preferences or it is used default value. */
    val updateInterval
        get() = getInt(R.string.key_sensor_location_update_interval, R.integer.default_sensor_location_update_interval) * 1000

    /** Returns maximal interval for location updates. Value is taken from shared preferences or it is used default value. */
    val maxUpdateInterval
        get() = getInt(R.string.key_sensor_location_max_update_interval, R.integer.default_sensor_location_max_update_interval)

    /** Returns priority of accuracy requests. Value is taken from shared preferences or it is used default value. */
    val accuracyPriority: Int
        get() = Integer.valueOf(getString(R.string.key_sensor_location_accuracy_priority, R.string.default_sensor_location_accuracy_priority))


    // ====================================== BATTERY SENSOR =======================================

    val isBatteryAllowed: Boolean
        get() = getBoolean(R.string.key_sensor_battery_is_allowed, R.bool.default_sensor_battery_is_allowed)
}