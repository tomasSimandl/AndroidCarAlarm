package com.example.tomas.carsecurity

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.support.v14.preference.PreferenceFragment
import android.view.MenuItem
import com.example.tomas.carsecurity.communication.SmsProvider
import com.example.tomas.carsecurity.preferenceFragments.MyPreferenceFragment
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.sensors.MoveDetector
import com.example.tomas.carsecurity.sensors.SoundDetector
import com.example.tomas.carsecurity.utils.Alarm
import com.example.tomas.carsecurity.utils.Tracker


class SettingsActivity : AppCompatPreferenceActivity() {

    /**
     * Set up the action bar if action bar is used.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)
//        val sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
//        sharedPreferences.edit().clear().apply()
    }

    /**
     * Handle back arrow click in action bar
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Return if preferences should be displayed in multi panel view.
     */
    override fun onIsMultiPane(): Boolean {
        return resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

    /**
     * Load preference headers.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * Method check if input fragment name is valid fragment which is allowed in this activity.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || ToolsPreferenceFragment::class.java.name == fragmentName
                || SensorsPreferenceFragment::class.java.name == fragmentName
                || CommunicationPreferenceFragment::class.java.name == fragmentName
                || PowerSaveModePreferenceFragment::class.java.name == fragmentName
    }

    /**
     * Method handle result of permission request.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if ((grantResults!!.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

            if (requestCode == R.string.key_sensor_sound_is_allowed
                    || requestCode == R.string.key_sensor_location_is_allowed
                    || requestCode == R.string.key_communication_sms_is_allowed
                    || requestCode == R.string.key_tool_alarm_is_call_allowed) {

                val sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                sharedPreferences.edit().putBoolean(getString(requestCode), true).apply()
            }
        }
    }


    /**
     * Class is used for preference screen for Tools/Utils
     */
    class ToolsPreferenceFragment : MyPreferenceFragment() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)

            addPreferencesFromResource(R.xml.pref_tools)

            // ALARM - preference check listener + set value
            registerPreferenceCheck(R.string.key_tool_alarm_is_allowed, Alarm)
            setValueToPreference(
                    R.string.key_tool_alarm_is_allowed,
                    resources.getBoolean(R.bool.default_tool_alarm_is_allowed),
                    Alarm)

            // CALL PROVIDER - preference check listener + set value
            registerPreferenceCheck(
                    R.string.key_tool_alarm_is_call_allowed,
                    CallProvider,
                    getString(R.string.pref_tool_alarm_telephony_permission_message),
                    arrayOf(Manifest.permission.CALL_PHONE))
            setValueToPreference(
                    R.string.key_tool_alarm_is_call_allowed,
                    resources.getBoolean(R.bool.default_tool_alarm_is_call_allowed),
                    CallProvider
            )


            // TRACKER - preference check listener + set value
            registerPreferenceCheck(R.string.key_tool_tracker_is_allowed, Tracker)
            setValueToPreference(
                    R.string.key_tool_tracker_is_allowed,
                    resources.getBoolean(R.bool.default_tool_tracker_is_allowed),
                    Tracker)
        }
    }

    /**
     * Class is used for preference screen for Sensors
     */
    class SensorsPreferenceFragment : MyPreferenceFragment() {
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

    /**
     * Class is used for preference screen for Communication
     */
    class CommunicationPreferenceFragment : MyPreferenceFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)

            addPreferencesFromResource(R.xml.pref_communication)

            // SMS - preference check listener + set value
            registerPreferenceCheck(
                    R.string.key_communication_sms_is_allowed,
                    SmsProvider,
                    getString(R.string.pref_communication_sms_permission_message),
                    arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
            setValueToPreference(
                    R.string.key_communication_sms_is_allowed,
                    resources.getBoolean(R.bool.default_communication_sms_is_allowed),
                    SmsProvider)
        }
    }

    /**
     * Class is used for preference screen for power save mode
     */
    class PowerSaveModePreferenceFragment : MyPreferenceFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)

            addPreferencesFromResource(R.xml.pref_power_save_mode)
        }
    }
}
