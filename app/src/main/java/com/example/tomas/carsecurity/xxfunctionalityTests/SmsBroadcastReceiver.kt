package com.example.tomas.carsecurity.xxfunctionalityTests

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony

class SmsBroadcastReceiver(private final val smsControlNumber: Set<String>) : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1!!.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION){

            var smsSender: String = ""
            var smsBody: String = ""

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(p1)) {
                    smsSender = smsMessage.displayOriginatingAddress
                    smsBody += smsMessage.messageBody
                }
            }



            println(smsSender)
            println(smsBody)
        }
    }

}