package com.example.tomas.carsecurity.fragments.preferences

import android.Manifest
import android.os.Bundle
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.sensors.MoveDetector
import com.example.tomas.carsecurity.sensors.SoundDetector

/**
 * Class is used for preference screen for Sensors
 */
class SensorsPreferenceFragment : MyPreferenceFragment() {

    /**
     * Method sets values to switch buttons and append listeners to change any value which disable these switches.
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        addPreferencesFromResource(R.xml.pref_sensors)

        // SOUND - preference check listener + set value
        registerPreferenceCheck(
                R.string.key_sensor_sound_is_allowed,
                SoundDetector,
                getString(R.string.pref_sensors_sound_audio_permission_message),
                arrayOf(Manifest.permission.RECORD_AUDIO))
        setValueToPreference(
                R.string.key_sensor_sound_is_allowed,
                resources.getBoolean(R.bool.default_sensor_sound_is_allowed),
                SoundDetector)

        // MOVE - preference check listener + set value
        registerPreferenceCheck(
                R.string.key_sensor_move_is_allowed,
                MoveDetector,
                "",
                arrayOf()) // no permissions needed
        setValueToPreference(
                R.string.key_sensor_move_is_allowed,
                resources.getBoolean(R.bool.default_sensor_move_is_allowed),
                MoveDetector)

        // LOCATION - preference check listener + set value
        registerPreferenceCheck(
                R.string.key_sensor_location_is_allowed,
                LocationProvider,
                getString(R.string.pref_sensors_location_fine_location_permission_message),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        setValueToPreference(
                R.string.key_sensor_location_is_allowed,
                resources.getBoolean(R.bool.default_sensor_location_is_allowed),
                LocationProvider)
    }
}