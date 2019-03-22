package com.example.tomas.carsecurity.fragments.preferences

import android.Manifest
import android.content.SharedPreferences
import android.os.Bundle
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.network.NetworkProvider
import com.example.tomas.carsecurity.communication.sms.SmsProvider

/**
 * Class is used for preference screen for Communication
 */
class CommunicationPreferenceFragment : MyPreferenceFragment() {

    /**
     * Method sets values to switch buttons and append listeners to change any value which disable these switches.
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        addPreferencesFromResource(R.xml.pref_communication)

        // SMS - preference check listener + set value
        registerPreferenceCheck(
                R.string.key_communication_sms_is_allowed,
                SmsProvider,
                getString(R.string.pref_communication_sms_permission_message),
                arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
        setValueToSmsSwitch()

        // Network - preference check listener + set value
        registerPreferenceCheck(
                R.string.key_communication_network_is_allowed,
                NetworkProvider,
                getString(R.string.pref_communication_network_permission_message),
                arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE))
        setValueToNetworkSwitch()
    }

    /**
     * Method check changes of phone number or network url. When change appears new value to allow switches is set.
     *
     * @param sharedPreferences is open [SharedPreferences] where change appears.
     * @param key is key of preference which was changed.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            getString(R.string.key_communication_sms_phone_number) -> setValueToSmsSwitch()
            getString(R.string.key_communication_network_url) -> setValueToNetworkSwitch()
        }
    }

    /**
     * Method set new value to allow sms switch.
     */
    private fun setValueToSmsSwitch() =
            setValueToPreference(
                    R.string.key_communication_sms_is_allowed,
                    resources.getBoolean(R.bool.default_communication_sms_is_allowed),
                    SmsProvider)

    /**
     * Method set new value to allow network switch.
     */
    private fun setValueToNetworkSwitch() = setValueToPreference(
            R.string.key_communication_network_is_allowed,
            resources.getBoolean(R.bool.default_communication_network_is_allowed),
            NetworkProvider)
}