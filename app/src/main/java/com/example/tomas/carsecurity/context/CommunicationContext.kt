package com.example.tomas.carsecurity.context

import android.content.Context
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.network.NetworkProvider
import com.example.tomas.carsecurity.communication.sms.SmsProvider

/**
 * Context contains data which are used in communication package and they are stored in
 * shared preferences or in resources.
 */
class CommunicationContext(appContext: Context) : BaseContext(appContext) {

    /** Contains default setting of sending of messages. Value is taken from resources. */
    private val defIsMsgAllowed: Boolean =
            appContext.resources.getBoolean(R.bool.default_communication_is_message_allowed)

    fun isProviderAllowed(provider: String): Boolean {
        return when (provider) {
            SmsProvider::class.java.name ->
                getBoolean(R.string.key_communication_sms_is_allowed,
                        R.bool.default_communication_sms_is_allowed)

            NetworkProvider::class.java.name ->
                getBoolean(R.string.key_communication_network_is_allowed,
                        R.bool.default_communication_network_is_allowed)

            else -> false
        }
    }

    fun isMessageAllowed(provider: String, vararg parameters: String): Boolean {
        val stringSet = when (provider) {

            SmsProvider::class.java.name ->
                getStringSet(R.string.key_communication_sms_allowed_message_types, null)

            NetworkProvider::class.java.name ->
                getStringSet(R.string.key_communication_network_allowed_message_types, null)

            else -> return false
        }
        return stringSet?.contains(parameters.joinToString("_")) ?: defIsMsgAllowed
    }

    var isLogin: Boolean
        get() = getBoolean(R.string.key_communication_network_is_user_login,
                R.bool.default_communication_network_is_user_login)

        set(value) = putBoolean(R.string.key_communication_network_is_user_login, value)

    /**
     * Returns phone number of contact person. Return value from sharedPreferences or empty string.
     */
    val phoneNumber: String
        get() = getString(R.string.key_communication_sms_phone_number, R.string.empty)

    /** Return url of server. Return value from sharedPreferences or empty string. */
    val serverUrl: String
        get() = getString(R.string.key_communication_network_server_url, R.string.empty)

    /** Return url of authorization server. Return value from sharedPreferences or empty string. */
    val authorizationServerUrl: String
        get() = getString(R.string.key_communication_network_auth_server_url, R.string.empty)

    /**
     * Indicates if can be used cellular for network synchronization.
     * Return value from sharedPreferences or empty string.
     */
    val cellular: Boolean
        get() = getBoolean(R.string.key_communication_network_cellular,
                R.bool.default_communication_network_cellular)

    /** Returns how often should by synchronize local database with server */
    val synchronizationInterval: Long
        get() = getInt(R.string.key_communication_network_update_interval,
                R.integer.default_communication_network_update_interval) * 60000L // *60 to second *1000 to milliseconds

    var firebaseToken: String
        get() = getString(R.string.key_communication_network_firebase_token, R.string.empty)
        set(value) = putString(R.string.key_communication_network_firebase_token, value)
}


