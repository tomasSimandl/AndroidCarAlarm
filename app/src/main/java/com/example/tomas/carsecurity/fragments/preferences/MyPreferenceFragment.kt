package com.example.tomas.carsecurity.fragments.preferences

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v14.preference.PreferenceFragment
import android.support.v14.preference.SwitchPreference
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.Preference
import com.example.tomas.carsecurity.*

open class MyPreferenceFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Method set shared preferences to specific instance of preferences
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = getString(R.string.preference_file_key)
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE

        setHasOptionsMenu(true)
    }

    /**
     * Listener of shared preferences. When value in shared preferences is changed view is updated.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val pref = findPreference(key)
        // used because compiler can not recognise that boot have isChecked attribute
        if (pref is SwitchPreference) {
            pref.isChecked = sharedPreferences.getBoolean(key, false)
        } else if (pref is CheckBoxPreference) {
            pref.isChecked = sharedPreferences.getBoolean(key, false)
        }
    }

    /**
     * Register this class to listen on changes in shared preferences. On change is run method
     * [onSharedPreferencesChanged()].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    /**
     * Remove registration of this class to changes in shared preferences.
     */
    override fun onDestroy() {
        super.onDestroy()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    /**
     * Method check if preference given by [prefKey] can be set to true by calling method check of
     * second parameter. If preference can be true value is taken from shared preferences or is
     * used default value given by [defValue]. Otherwise is preference set to false.
     *
     * @param prefKey preference key. Must be [SwitchPreference]
     * @param defValue default value which is used only when preference is not set but preference can be true
     * @param checkObj for correct usage must be [CheckObjByte] or [CheckObjString]
     */
    protected fun setValueToPreference(prefKey: Int, defValue: Boolean, checkObj: Any) {
        val preference = findPreference(getString(prefKey))

        if (preference is SwitchPreference || preference is CheckBoxPreference) {

            val canBeTrue = when (checkObj) {
                is CheckObjString -> checkObj.check(activity, true).isEmpty()
                is CheckObjByte -> checkObj.check(activity) == CheckCodes.success
                else -> false
            }

            val value = if (canBeTrue) {
                preferenceScreen.sharedPreferences.getBoolean(getString(prefKey), defValue)
            } else {
                false
            }

            // used because compiler can not recognise that boot have isChecked attribute
            if (preference is SwitchPreference) {
                preference.isChecked = value
            } else if (preference is CheckBoxPreference) {
                preference.isChecked = value
            }
        }
    }


    /**
     * Method find preference by given [prefKey] and register
     * [android.preference.Preference.OnPreferenceChangeListener]. Listener checks if new value
     * of preference is true, check method of [checkObj] is called and according to result is shown
     * message or value is changed to true.
     *
     * @param prefKey preference key. Must be [SwitchPreference]
     * @param checkObj Class which is associated with preference key
     */
    protected fun registerPreferenceCheck(prefKey: Int, checkObj: CheckObjString){

        findPreference(getString(prefKey))?.setOnPreferenceChangeListener { _: Preference, value: Any ->
            if (value == true) {
                val msg = checkObj.check(activity, true)

                if (msg.isNotBlank()) {
                    showMessage(msg, getString(R.string.error_msg_title), DialogInterface.OnClickListener { _, _ -> }, null)
                    return@setOnPreferenceChangeListener false
                }
            }
            return@setOnPreferenceChangeListener true
        }
    }

    /**
     * Method find preference by given [prefKey] and register
     * [android.preference.Preference.OnPreferenceChangeListener]. Listener checks if new value
     * of preference is true, check method of [checkObj] is called and according to result is shown
     * message, permission request or value is changed to true.
     *
     * @param prefKey preference key. Must be [SwitchPreference]
     * @param checkObj Class which is associated with preference key
     * @param permMsg message which is describing why permission is needed
     * @param permissions array of requested permissions
     */
    protected fun registerPreferenceCheck(prefKey: Int, checkObj: CheckObjByte, permMsg: String, permissions: Array<String>) {

        findPreference(getString(prefKey))?.setOnPreferenceChangeListener { _: Preference, value: Any ->
            if (value == true) {

                val checkCode = checkObj.check(activity)

                when (checkCode) {
                    CheckCodes.invalidParameters -> showMessage(getString(R.string.error_invalid_params), getString(R.string.error_msg_title), DialogInterface.OnClickListener { _, _ ->  }, null)
                    CheckCodes.hardwareNotSupported -> showMessage(getString(R.string.error_hw_not_supported), getString(R.string.error_msg_title), DialogInterface.OnClickListener { _, _ ->  }, null)
                    CheckCodes.permissionDenied -> askForPermission(prefKey, permMsg, permissions)
                    else -> return@setOnPreferenceChangeListener true
                }
                return@setOnPreferenceChangeListener false
            }
            return@setOnPreferenceChangeListener true
        }
    }

    /**
     * Method is used for displaying permission requests according to given parameters. When it is
     * necessary description message is displayed before before permission request. Result of
     * request is processed by [SettingsActivity].
     *
     * @param code preference key which is used as permission request code
     * @param permMsg message which is describing why permission is needed
     * @param permissions array of requested permissions
     */
    private fun askForPermission(code: Int, permMsg: String, permissions: Array<String> ) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)) {
            showMessage(permMsg,"Permission request",
                    DialogInterface.OnClickListener { _, _ -> ActivityCompat.requestPermissions(activity, permissions, code)},
                    DialogInterface.OnClickListener { _, _ -> }
            )
        } else {
            ActivityCompat.requestPermissions(activity, permissions, code)
        }
    }

    /**
     * Method create and show alert dialog according to given parameters.
     *
     * @param msg message of alert dialog
     * @param title of alert dialog
     * @param positiveButton if null button is not displayed otherwise is displayed OK button
     * @param negativeButton if null button is not displayed otherwise is displayed CANCEL button
     */
    private fun showMessage(msg: String, title: String, positiveButton: DialogInterface.OnClickListener?, negativeButton: DialogInterface.OnClickListener?) {
        val dialog = AlertDialog.Builder(activity)
                .setMessage(msg)
                .setTitle(title)

        if (positiveButton != null) dialog.setPositiveButton(R.string.ok, positiveButton)
        if(negativeButton != null) dialog.setNegativeButton(R.string.cancel, negativeButton)

        dialog.create().show()
    }
}