package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.sensors.MoveDetector
import com.example.tomas.carsecurity.sensors.SoundDetector
import com.example.tomas.carsecurity.utils.UtilsManager

/**
 * Context contains data which are used in [UtilsManager] class and they are stored in
 * shared preferences or in resources.
 */
class UtilsContext(private val sharedPreferences: SharedPreferences, private val context: Context) {

    /** Contains if [LocationProvider] can be in default used. Value is taken from resources. */
    private val isLocationProviderDef :Boolean = context.resources.getBoolean(R.bool.default_util_is_location_provider_available)
    /** Returns if [LocationProvider] can be in default used. Value is taken from shared preferences or it is used default value. */
    val isLocationProvider
        get() = sharedPreferences.getBoolean(context.getString(R.string.key_util_is_location_provider_available), isLocationProviderDef)

    /** Contains if [MoveDetector] can be in default used. Value is taken from resources. */
    private val isMoveDetectorDef :Boolean = context.resources.getBoolean(R.bool.default_util_is_move_detector_available)
    /** Returns if [MoveDetector] can be in default used. Value is taken from shared preferences or it is used default value. */
    val isMoveDetector
        get() = sharedPreferences.getBoolean(context.getString(R.string.key_util_is_move_detector_available), isMoveDetectorDef)

    /** Contains if [SoundDetector] can be in default used. Value is taken from resources. */
    private val isSoundDetectorDef :Boolean = context.resources.getBoolean(R.bool.default_util_is_sound_detector_available)
    /** Returns if [SoundDetector] can be in default used. Value is taken from shared preferences or it is used default value. */
    val isSoundDetector
        get() = sharedPreferences.getBoolean(context.getString(R.string.key_util_is_sound_detector_available), isSoundDetectorDef)

}


