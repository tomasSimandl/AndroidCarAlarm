package com.example.tomas.carsecurity.communication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.context.SmsProviderContext
import com.example.tomas.carsecurity.utils.UtilsEnum
import com.google.android.gms.common.util.Strings


class SmsBroadcastReceiver : BroadcastReceiver() {

    private val tag = "SmsBroadcastReceiver"

    private lateinit var smsProviderContext: SmsProviderContext


    override fun onReceive(context: Context, intent: Intent) {
        if (! ::smsProviderContext.isInitialized) {
            val sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            smsProviderContext = SmsProviderContext(sharedPreferences, context.applicationContext)
        }

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

        val phoneNumber = smsProviderContext.phoneNumber

        if(Strings.isEmptyOrWhitespace(phoneNumber)){
            Log.w(tag, "Contact phone number is not set.")
            return
        }

        if (phoneNumber != smsSender) { // TODO +420
            Log.d(tag, "Phone number of incoming message is not allowed to control app.")
            return
        }

        when {
            smsBody.startsWith("activate", true) -> switchUtil(smsBody.drop(8).trim(), true)
            smsBody.startsWith("deactivate", true) -> switchUtil(smsBody.drop(10).trim(), false)
            smsBody == "info" -> sendIntent(MainService.Actions.ActionStatus.name)
            smsBody == "position" -> sendIntent(MainService.Actions.ActionGetPosition.name)
        }
    }

    private fun switchUtil(smsBody: String, activate: Boolean){
        try {
            val util = UtilsEnum.valueOf(smsBody)

            val intent = Intent(smsProviderContext.context, MainService::class.java)
            intent.action = if(activate) MainService.Actions.ActionActivateUtil.name else MainService.Actions.ActionDeactivateUtil.name
            intent.putExtra("util", util)
            smsProviderContext.context.startService(intent)

            Log.d(tag, "Intent was sent.")

        } catch (e: IllegalArgumentException){
            Log.d(tag, "Incoming command had invalid util name")
        }
    }

    private fun sendIntent(action: String){
        val intent = Intent(smsProviderContext.context, MainService::class.java)
        intent.action = action
        smsProviderContext.context.startService(intent)
    }

}