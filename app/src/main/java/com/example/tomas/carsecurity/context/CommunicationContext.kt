package com.example.tomas.carsecurity.context

import android.content.Context
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.SmsProvider

/**
 * Context contains data which are used in communication package and they are stored in
 * shared preferences or in resources.
 */
class CommunicationContext(appContext: Context): BaseContext(appContext) {

    /** Contains default setting of sending of messages. Value is taken from resources. */
    private val defIsMsgAllowed :Boolean = appContext.resources.getBoolean(R.bool.default_communication_is_message_allowed)

    fun isProviderAllowed(provider: String): Boolean{
        return when(provider){
            SmsProvider::class.java.name -> getBoolean(R.string.key_communication_sms_is_allowed, R.bool.default_communication_sms_is_allowed)
            else -> false
        }
    }

    fun isMessageAllowed(provider: String, vararg parameters: String): Boolean{
        val stringSet = when(provider){
            SmsProvider::class.java.name -> getStringSet(R.string.key_communication_sms_allowed_message_types,null)
            else -> return false
        }
        return stringSet?.contains(parameters.joinToString("_")) ?: defIsMsgAllowed
    }

    /** Returns phone number of contact person. Return value from sharedPreferences or empty string. */
    val phoneNumber: String
        get() = getString(R.string.key_communication_sms_phone_number, R.string.empty)
}


