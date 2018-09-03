package com.example.tomas.carsecurity

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.preference.RingtonePreference
import android.support.v14.preference.PreferenceFragment
import android.text.TextUtils
import android.view.MenuItem
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceGroup
import android.view.View
import com.example.tomas.carsecurity.context.AlarmContext
import com.example.tomas.carsecurity.context.TrackerContext
import com.example.tomas.carsecurity.utils.Alarm
import com.pavelsikun.seekbarpreference.SeekBarPreferenceCompat

class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
        
        val sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key),Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
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
//                || SensorsPreferenceFragment::class.java.name == fragmentName
//                || CommunicationPreferenceFragment::class.java.name == fragmentName
    }









    class ToolsPreferenceFragment : PreferenceFragment() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

            preferenceManager.sharedPreferencesName = getString(R.string.preference_file_key)
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE

            addPreferencesFromResource(R.xml.pref_tools)
            setHasOptionsMenu(true)

        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }


//    class SensorsPreferenceFragment : PreferenceFragment() {
//        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//            addPreferencesFromResource(R.xml.pref_sensors)
//            setHasOptionsMenu(true)
//        }
//
//        override fun onOptionsItemSelected(item: MenuItem): Boolean {
//            val id = item.itemId
//            if (id == android.R.id.home) {
//                startActivity(Intent(activity, SettingsActivity::class.java))
//                return true
//            }
//            return super.onOptionsItemSelected(item)
//        }
//    }
//
//    class CommunicationPreferenceFragment : PreferenceFragment() {
//        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//            addPreferencesFromResource(R.xml.pref_communication)
//            setHasOptionsMenu(true)
//        }
//
//        override fun onOptionsItemSelected(item: MenuItem): Boolean {
//            val id = item.itemId
//            if (id == android.R.id.home) {
//                startActivity(Intent(activity, SettingsActivity::class.java))
//                return true
//            }
//            return super.onOptionsItemSelected(item)
//        }
//    }

//    companion object {
//
//        /**
//         * A preference value change listener that updates the preference's summary
//         * to reflect its new value.
//         */
//        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
//            val stringValue = value.toString()
//
//            if (preference is ListPreference) {
//                // For list preferences, look up the correct display value in
//                // the preference's 'entries' list.
//                val listPreference = preference
//                val index = listPreference.findIndexOfValue(stringValue)
//
//                // Set the summary to reflect the new value.
//                preference.setSummary(
//                        if (index >= 0)
//                            listPreference.entries[index]
//                        else
//                            null)
//
//            } else if (preference is RingtonePreference) {
//                // For ringtone preferences, look up the correct display value
//                // using RingtoneManager.
//                if (TextUtils.isEmpty(stringValue)) {
//                    // Empty values correspond to 'silent' (no ringtone).
//                    preference.setSummary(R.string.pref_ringtone_silent)
//
//                } else {
//                    val ringtone = RingtoneManager.getRingtone(
//                            preference.getContext(), Uri.parse(stringValue))
//
//                    if (ringtone == null) {
//                        // Clear the summary if there was a lookup error.
//                        preference.setSummary(null)
//                    } else {
//                        // Set the summary to reflect the new ringtone display
//                        // name.
//                        val name = ringtone.getTitle(preference.getContext())
//                        preference.setSummary(name)
//                    }
//                }
//
//            } else {
//                // For all other preferences, set the summary to the value's
//                // simple string representation.
//                preference.summary = stringValue
//            }
//            true
//        }
//
//        /**
//         * Binds a preference's summary to its value. More specifically, when the
//         * preference's value is changed, its summary (line of text below the
//         * preference title) is updated to reflect the value. The summary is also
//         * immediately updated upon calling this method. The exact display format is
//         * dependent on the type of preference.
//
//         * @see .sBindPreferenceSummaryToValueListener
//         */
//        private fun bindPreferenceSummaryToValue(preference: Preference) {
//            // Set the listener to watch for value changes.
//            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
//
//            // Trigger the listener immediately with the preference's
//            // current value.
//            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
//                    PreferenceManager
//                            .getDefaultSharedPreferences(preference.context)
//                            .getString(preference.key, ""))
//        }
//    }
}
