package com.example.tomas.carsecurity

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.support.v14.preference.PreferenceFragment
import com.example.tomas.carsecurity.preferenceFragments.MyPreferenceFragment

class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()

//        val sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key),Context.MODE_PRIVATE)
//        sharedPreferences.edit().clear().apply()
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onIsMultiPane(): Boolean {
        return resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || ToolsPreferenceFragment::class.java.name == fragmentName
                || SensorsPreferenceFragment::class.java.name == fragmentName
//                || CommunicationPreferenceFragment::class.java.name == fragmentName
    }









    class ToolsPreferenceFragment : MyPreferenceFragment() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)

            addPreferencesFromResource(R.xml.pref_tools)
        }
    }


    class SensorsPreferenceFragment : MyPreferenceFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)

            addPreferencesFromResource(R.xml.pref_sensors)
        }
    }

    class CommunicationPreferenceFragment : MyPreferenceFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)

            addPreferencesFromResource(R.xml.pref_communication)
        }
    }
}
