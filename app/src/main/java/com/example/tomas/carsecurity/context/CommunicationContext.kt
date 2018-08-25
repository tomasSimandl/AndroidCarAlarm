package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.CommunicationManager

/**
 * Context contains data which are used in [CommunicationManager] class and they are stored in
 * shared preferences or in resources.
 */
class CommunicationContext(private val sharedPreferences: SharedPreferences, private val context: Context) {

    /** Contains default setting of sending of messages. Value is taken from resources. */
    private val defCanSendMessage :Boolean = context.resources.getBoolean(R.bool.default_communication_can_send_message)


    fun canSendMessage(provider: String, util: String, msgType: String): Boolean{
        return sharedPreferences.getBoolean("""communication_${provider}_${util}_${msgType}_can_send""", defCanSendMessage)
    }

    fun canSendMessage(provider: String, msgType: String): Boolean{
        return sharedPreferences.getBoolean("""communication_${provider}_${msgType}_can_send""", defCanSendMessage)
    }
}


