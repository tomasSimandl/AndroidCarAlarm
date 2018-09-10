package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.SmsProvider

/**
 * Context contains data which are used in communication package and they are stored in
 * shared preferences or in resources.
 */
class CommunicationContext(val sharedPreferences: SharedPreferences, val context: Context) {

    /** Contains default setting of sending of messages. Value is taken from resources. */
    private val defIsMsgAllowed :Boolean = context.resources.getBoolean(R.bool.default_communication_is_message_allowed)

    fun isProviderAllowed(provider: String): Boolean{

        val keysId = when(provider){
            SmsProvider::class.java.name -> arrayOf(R.string.key_communication_sms_is_allowed, R.bool.default_communication_sms_is_allowed)
            else -> return false
        }

        return sharedPreferences.getBoolean(context.resources.getString(keysId[0]), context.resources.getBoolean(keysId[1]))
    }

    fun isMessageAllowed(provider: String, vararg parameters: String): Boolean{
        val stringSet = when(provider){
            SmsProvider::class.java.name -> sharedPreferences.getStringSet(context.resources.getString(R.string.key_communication_sms_allowed_message_types),null)
            else -> return false
        }

        return stringSet?.contains(parameters.joinToString("_")) ?: defIsMsgAllowed
    }

    /** Returns phone number of contact person. Return value from sharedPreferences or empty string. */
    val phoneNumber: String
        get() = sharedPreferences.getString(context.getString(R.string.key_communication_sms_phone_number), "")
}


