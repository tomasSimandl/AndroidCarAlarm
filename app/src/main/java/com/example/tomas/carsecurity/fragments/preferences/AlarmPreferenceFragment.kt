package com.example.tomas.carsecurity.fragments.preferences

import android.Manifest
import android.os.Bundle
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.tools.Alarm
import com.example.tomas.carsecurity.utils.CallProvider

/**
 * Class is used for preference screen for Alarm
 */
class AlarmPreferenceFragment : MyPreferenceFragment() {

    /**
     * Method sets values to switch buttons and append listeners to change any value which disable these switches.
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        addPreferencesFromResource(R.xml.pref_alarm)

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
    }
}