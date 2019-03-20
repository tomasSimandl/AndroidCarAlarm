package com.example.tomas.carsecurity.fragments.preferences

import android.os.Bundle
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.tools.Tracker

/**
 * Class is used for preference screen for Tracker
 */
class TrackerPreferenceFragment : MyPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        addPreferencesFromResource(R.xml.pref_tracker)

        // TRACKER - preference check listener + set value
        registerPreferenceCheck(R.string.key_tool_tracker_is_allowed, Tracker)
        setValueToPreference(
                R.string.key_tool_tracker_is_allowed,
                resources.getBoolean(R.bool.default_tool_tracker_is_allowed),
                Tracker)
    }
}