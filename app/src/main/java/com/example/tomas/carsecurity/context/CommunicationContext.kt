package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R

/**
 * Context contains data which are used in communication package and they are stored in
 * shared preferences or in resources.
 */
class CommunicationContext(private val sharedPreferences: SharedPreferences, val context: Context) {

    /** Contains default setting of sending of messages. Value is taken from resources. */
    private val defCanSendMessage :Boolean = context.resources.getBoolean(R.bool.default_communication_can_send_message)

    private val defActiveProviders = context.resources.getStringArray(R.array.default_communication_active_providers).toHashSet()


    fun canSendMessage(provider: String, util: String, msgType: String): Boolean{
        return sharedPreferences.getBoolean("""communication_${provider}_${util}_${msgType}_can_send""", defCanSendMessage)
    }

    fun canSendMessage(provider: String, msgType: String): Boolean{
        return sharedPreferences.getBoolean("""communication_${provider}_${msgType}_can_send""", defCanSendMessage)
    }

    val activeProviders: Set<String>
        get() = sharedPreferences.getStringSet(context.getString(R.string.key_communication_active_providers), defActiveProviders)

    /** Returns phone number of contact person. Return value from sharedPreferences or empty string. */
    val phoneNumber: String
        get() = sharedPreferences.getString(context.getString(R.string.key_communication_sms_phone_number), "")
}


