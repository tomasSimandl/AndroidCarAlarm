package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R

class SoundDetectorContext(private val sharedPreferences: SharedPreferences, private val context: Context) {

    /** Contains default value from resources for maximal allowed amplitude before alarm is triggered. */
    private val defMaxAmplitude = context.resources.getInteger(R.integer.default_max_ok_amplitude)
    /** Contains default value from resources for measure interval. */
    private val defMeasureInterval = context.resources.getInteger(R.integer.default_sound_detector_interval)

    /** Return maximal allowed amplitude before alarm is triggered. Value is from shared preferences or it is default value. */
    val maxAmplitude
        get() = sharedPreferences.getInt(context.getString(R.string.key_max_ok_amplitude), defMaxAmplitude)

    /** Return measure interval from shared preferences or use default value */
    val measureInterval
        get() = sharedPreferences.getLong(context.getString(R.string.key_sound_detector_interval), defMeasureInterval.toLong())
}
