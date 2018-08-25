package com.example.tomas.carsecurity.communication

import android.telephony.SmsManager
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.UtilsEnum
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

    override fun sendUtilSwitch(utilsEnum: UtilsEnum, enabled: Boolean): Boolean {

        if(context.communicationContext.canSendMessage(this.javaClass.name, utilsEnum.name, MessageType.UtilSwitch.name)){

            val text = if(enabled)
                context.appContext.getString(R.string.sms_util_enabled)
            else
                context.appContext.getString(R.string.sms_util_disabled)

            return sendMessage(text)
        }
        return false
    }
}
