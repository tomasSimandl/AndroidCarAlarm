package com.example.tomas.carsecurity.communication

import android.telephony.SmsManager
import com.example.tomas.carsecurity.context.MyContext
import com.google.android.gms.common.util.Strings

class SmsProvider(private val context: MyContext) : ICommunicationProvider {

    private val smsManager = SmsManager.getDefault()

    override fun sendMessage(text: String): Boolean {
        if(Strings.isEmptyOrWhitespace(context.smsProviderContext.phoneNumber) || Strings.isEmptyOrWhitespace(text)){
            return false
        }

        val textParts = smsManager.divideMessage(text)

        if (textParts.size > 1) {
            smsManager.sendMultipartTextMessage(context.smsProviderContext.phoneNumber,null, textParts, null, null)
        } else {
            smsManager.sendTextMessage(context.smsProviderContext.phoneNumber, null, text, null, null)
        }
        return true
    }
}