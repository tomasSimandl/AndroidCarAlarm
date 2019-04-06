package com.example.tomas.carsecurity.communication.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsMessage
import android.util.Log
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.communication.MessageType
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.tools.ToolsEnum
import com.google.android.gms.common.util.Strings

/**
 * Class is used for receiving incoming SMS messages.
 */
class SmsBroadcastReceiver(private val communicationContext: CommunicationContext) : BroadcastReceiver() {

    /** Logger tag */
    private val tag = "SmsBroadcastReceiver"

    /**
     * Method is automatically called when new SMS message was received. Phone number of sender and text of SMS is
     * extract from input intent and forwarded to processMessage method.
     */
    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {

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
                            @Suppress("DEPRECATION")
                            message = SmsMessage.createFromPdu(pdus[i] as ByteArray) // Deprecated in API 23 (i use it only on api < 19)

                            smsBody += message.messageBody
                            smsSender = message.originatingAddress ?: ""
                        }
                    }
                }
            }
            processMessage(smsSender.trim(), smsBody.trim())
        }
    }

    /**
     * Method decode incoming message and decide which command should be perform.
     * When phone number is not the phone number given by user in setting message is ignored.
     *
     * @param smsSender phone number of sender
     * @param smsBody whole body of sms
     */
    private fun processMessage(smsSender: String, smsBody: String) {

        val phoneNumber = communicationContext.phoneNumber

        if (Strings.isEmptyOrWhitespace(phoneNumber)) {
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
                if (communicationContext.isMessageAllowed(SmsProvider::class.java.name, MessageType.Status.name, "recv")) {
                    sendIntentStatus()
                }
        }
    }

    /**
     * Method check if incoming command is allowed and than send intent to MainService with activate or deactivate
     * util command.
     *
     * @param smsBody sms message body which should include only util name
     * @param activate specify if it is activation command - true or deactivation command - false
     */
    private fun switchUtil(smsBody: String, activate: Boolean) {
        if (!communicationContext.isMessageAllowed(SmsProvider::class.java.name, MessageType.UtilSwitch.name, "recv")) {
            Log.d(tag, "Util switch command is not allowed.")
            return
        }
        try {
            val util = ToolsEnum.valueOf(smsBody)

            val intent = Intent(communicationContext.appContext, MainService::class.java)
            intent.action = if (activate) MainService.Actions.ActionActivateUtil.name else MainService.Actions.ActionDeactivateUtil.name
            intent.putExtra("util", util)
            communicationContext.appContext.startService(intent)

            Log.d(tag, "Intent was sent.")

        } catch (e: IllegalArgumentException) {
            Log.d(tag, "Incoming command had invalid util name")
        }
    }

    /**
     * Method send intent to MainService with Send status action. To message is appended information defines which
     * provider should respond.
     */
    private fun sendIntentStatus() {
        val intent = Intent(communicationContext.appContext, MainService::class.java)
        intent.action = MainService.Actions.ActionStatus.name
        intent.putExtra("communicator", SmsProvider::class.hashCode())
        communicationContext.appContext.startService(intent)
    }

    /**
     * Method send intent to MainService with given action.
     *
     * @param action string which represents action which should be perform.
     */
    private fun sendIntent(action: String) {
        val intent = Intent(communicationContext.appContext, MainService::class.java)
        intent.action = action
        communicationContext.appContext.startService(intent)
    }
}