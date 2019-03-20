package com.example.tomas.carsecurity.fragments.preferences

import android.os.Bundle
import com.example.tomas.carsecurity.R

/**
 * Class is used for preference screen for power save mode
 */
class PowerSaveModePreferenceFragment : MyPreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        addPreferencesFromResource(R.xml.pref_power_save_mode)
    }
}