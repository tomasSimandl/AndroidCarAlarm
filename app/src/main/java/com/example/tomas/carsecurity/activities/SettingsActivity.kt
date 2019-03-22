package com.example.tomas.carsecurity.activities

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.support.v14.preference.PreferenceFragment
import android.view.MenuItem
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.fragments.preferences.*

/**
 * This class represents activity with application settings.
 */
class SettingsActivity : AppCompatPreferenceActivity() {

    /**
     * Set up the action bar if action bar is used.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Handle back arrow click in action bar
     *
     * @param item is menuItem which was selected.
     * @return true if action was handled, false otherwise.
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
     * @return true if preferences should be displayed in multi panel view.
     */
    override fun onIsMultiPane(): Boolean {
        return resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

    /**
     * Loads preference headers.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * Method check if input [fragmentName] is valid fragment which is allowed in this activity.
     *
     * @param fragmentName class name of fragment
     * @return true if fragment is allowed, false otherwise
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || TrackerPreferenceFragment::class.java.name == fragmentName
                || AlarmPreferenceFragment::class.java.name == fragmentName
                || SensorsPreferenceFragment::class.java.name == fragmentName
                || CommunicationPreferenceFragment::class.java.name == fragmentName
                || PowerSaveModePreferenceFragment::class.java.name == fragmentName
    }

    /**
     * Method handle results of permission requests.
     *
     * @param requestCode identification number of our request
     * @param permissions list of requested permissions
     * @param grantResults list of permission request results.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

            if (requestCode == R.string.key_sensor_sound_is_allowed
                    || requestCode == R.string.key_sensor_location_is_allowed
                    || requestCode == R.string.key_communication_sms_is_allowed
                    || requestCode == R.string.key_communication_network_is_allowed
                    || requestCode == R.string.key_tool_alarm_is_call_allowed) {

                val sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                sharedPreferences.edit().putBoolean(getString(requestCode), true).apply()
            }
        }
    }
}
