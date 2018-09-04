package com.example.tomas.carsecurity.communication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsMessage
import android.util.Log
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.utils.UtilsEnum
import com.google.android.gms.common.util.Strings


class SmsBroadcastReceiver(private val communicationContext: CommunicationContext) : BroadcastReceiver() {

    private val tag = "SmsBroadcastReceiver"

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION){

            var smsSender = ""
            var smsBody = ""

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    smsSender = smsMessage.displayOriginatingAddress
                    smsBody += smsMessage.messageBody
                }
            } else {

                val smsBundle = intent.extras
                if (smsBundle != null) {

                    val bundlePdus = smsBundle["pdus"]
                    if (bundlePdus == null) {
                        Log.e(tag, "Sms bundle is malformed")

                    } else {

                        val pdus = bundlePdus as Array<*>
                        var message: SmsMessage

                        for (i in pdus.indices) {
                            message = SmsMessage.createFromPdu(pdus[i] as ByteArray) // Deprecated in API 23 (i use it only on api < 19)

                            smsBody += message.messageBody
                            smsSender = message.originatingAddress
                        }
                    }
                }
            }

            processMessage(smsSender.trim(), smsBody.trim())
        }
    }

    private fun processMessage(smsSender: String, smsBody: String){

        val phoneNumber = communicationContext.phoneNumber

        if(Strings.isEmptyOrWhitespace(phoneNumber)){
            Log.w(tag, "Contact phone number is not set.")
            return
        }

        if (!PhoneNumberUtils.compare(phoneNumber, smsSender)) {
            Log.d(tag, "Phone number of incoming message is not allowed to control app.")
            return
        }

        when {
            smsBody.startsWith("activate", true) -> switchUtil(smsBody.drop(8).trim(), true)
            smsBody.startsWith("deactivate", true) -> switchUtil(smsBody.drop(10).trim(), false)
            smsBody == "info" ->
                if(communicationContext.isMessageAllowed(SmsProvider::class.java.name, MessageType.Status.name, "recv")) {
                    sendIntent(MainService.Actions.ActionStatus.name)
                }
            smsBody == "position" ->
                if(communicationContext.isMessageAllowed(SmsProvider::class.java.name, MessageType.Location.name, "recv")) {
                    sendIntent(MainService.Actions.ActionGetPosition.name)
                }
        }
    }

    private fun switchUtil(smsBody: String, activate: Boolean){
        if(!communicationContext.isMessageAllowed(SmsProvider::class.java.name, MessageType.UtilSwitch.name, "recv")){
            Log.d(tag, "Util switch command is not allowed.")
            return
        }
        try {
            val util = UtilsEnum.valueOf(smsBody)

            val intent = Intent(communicationContext.context, MainService::class.java)
            intent.action = if(activate) MainService.Actions.ActionActivateUtil.name else MainService.Actions.ActionDeactivateUtil.name
            intent.putExtra("util", util)
            communicationContext.context.startService(intent)

            Log.d(tag, "Intent was sent.")

        } catch (e: IllegalArgumentException){
            Log.d(tag, "Incoming command had invalid util name")
        }
    }

    private fun sendIntent(action: String){
        val intent = Intent(communicationContext.context, MainService::class.java)
        intent.action = action
        communicationContext.context.startService(intent)
    }
}