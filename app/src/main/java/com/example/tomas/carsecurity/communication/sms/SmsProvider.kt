package com.example.tomas.carsecurity.communication.sms

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.telephony.SmsManager
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjByte
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.ICommunicationProvider
import com.example.tomas.carsecurity.communication.MessageType
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.utils.UtilsEnum
import com.google.android.gms.common.util.Strings
import java.util.*

class SmsProvider(private val communicationContext: CommunicationContext) : ICommunicationProvider {

    private val tag = "SmsProvider"
    private lateinit var smsManager: SmsManager
    private lateinit var smsBroadcastReceiver: SmsBroadcastReceiver
    private var isInitialize = false

    companion object Check : CheckObjByte {
        override fun check(context: Context): Byte {
            return if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                CheckCodes.hardwareNotSupported
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                CheckCodes.permissionDenied
            } else if (CommunicationContext(context).phoneNumber.isBlank()) {
                CheckCodes.invalidParameters
            } else if (!CommunicationContext(context).isProviderAllowed(SmsProvider::class.java.name)) {
                CheckCodes.notAllowed
            } else {
                CheckCodes.success
            }
        }
    }

    override fun initialize(): Boolean {
        if (check(communicationContext.appContext) == CheckCodes.success) {

            smsManager = SmsManager.getDefault()
            smsBroadcastReceiver = SmsBroadcastReceiver(communicationContext)

            val intentFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
            communicationContext.appContext.registerReceiver(smsBroadcastReceiver, intentFilter)
            isInitialize = true
            return true
        }
        isInitialize = false
        return false
    }

    override fun destroy() {
        if (isInitialize) {
            communicationContext.appContext.unregisterReceiver(smsBroadcastReceiver)
            isInitialize = false
        }
    }

    override fun isInitialize(): Boolean {
        return isInitialize
    }


    private fun sendMessage(text: String): Boolean {
        if (check(communicationContext.appContext) != CheckCodes.success) {
            Log.d(tag, "Can not send SMS. Permission not granted or unsupported hardware")
            return false
        }

        if (Strings.isEmptyOrWhitespace(communicationContext.phoneNumber) || Strings.isEmptyOrWhitespace(text)) {
            Log.d(tag, "Empty phone number or text of message is empty")
            return false
        }

        val textParts = smsManager.divideMessage(text)

        if (textParts.size > 1) {
            Log.d(tag, "Sending multipart message.")
            smsManager.sendMultipartTextMessage(communicationContext.phoneNumber, null, textParts, null, null)
        } else {
            Log.d(tag, "Sending message: $text")
            smsManager.sendTextMessage(communicationContext.phoneNumber, null, text, null, null)
        }
        return true
    }

    override fun sendUtilSwitch(utilsEnum: UtilsEnum, enabled: Boolean): Boolean {

        return if (communicationContext.isMessageAllowed(this.javaClass.name, utilsEnum.name, MessageType.UtilSwitch.name, "send")) {
            Log.d(tag, "Sending util switch SMS message of util: ${utilsEnum.name}.")

            val text = if (enabled)
                communicationContext.appContext.getString(R.string.sms_util_enabled, utilsEnum.name)
            else
                communicationContext.appContext.getString(R.string.sms_util_disabled, utilsEnum.name)

            Log.d(tag, """Sending sms util switch message of util: ${utilsEnum.name}""")
            sendMessage(text)

        } else {
            Log.d(tag, "Util switch SMS message is not allowed for util ${utilsEnum.name}.")
            false
        }
    }

    override fun sendEvent(messageType: MessageType, vararg args: String): Boolean {
        return when (messageType) {
            MessageType.Alarm -> sendAlarm()
            MessageType.BatteryWarn -> sendBatteryWarn(args.first())
            MessageType.PowerConnected -> sendPowerConnected(args.first())
            MessageType.PowerDisconnected -> sendPowerDisconnected(args.first())
            else -> {
                Log.e(tag, "Sending SMS of message type $messageType is not supported as Event sending.")
                false
            }
        }
    }

    private fun sendAlarm(): Boolean{
        return if(communicationContext.isMessageAllowed(this.javaClass.name, MessageType.Alarm.name, "send")){
            Log.d(tag, "Sending alarm sms message.")
            sendMessage(communicationContext.appContext.getString(R.string.sms_alarm, Calendar.getInstance().time.toString()))
        } else {
            Log.d(tag, "Sms message for alarm is not allowed.")
            false
        }
    }

    private fun sendBatteryWarn(capacity: String): Boolean {
        return if(communicationContext.isMessageAllowed(this.javaClass.name, MessageType.BatteryWarn.name, "send")){
            Log.d(tag, "Sending battery warning sms message.")
            sendMessage(communicationContext.appContext.getString(R.string.sms_battery_warn, capacity))
        } else {
            Log.d(tag, "Battery warning sms message is not allowed.")
            false
        }
    }

    private fun sendPowerConnected(capacity: String): Boolean {
        return if(communicationContext.isMessageAllowed(this.javaClass.name, MessageType.PowerConnected.name, "send")){
            Log.d(tag, "Sending power connected warning sms message.")
            sendMessage(communicationContext.appContext.getString(R.string.sms_power_connected, capacity))
        } else {
            Log.d(tag, "Power connected warning sms message is not allowed.")
            false
        }
    }

    private fun sendPowerDisconnected(capacity: String): Boolean {
        return if(communicationContext.isMessageAllowed(this.javaClass.name, MessageType.PowerDisconnected.name, "send")){
            Log.d(tag, "Sending power disconnected warning sms message.")
            sendMessage(communicationContext.appContext.getString(R.string.sms_power_disconnected, capacity))
        } else {
            Log.d(tag, "Power disconnected warning sms message is not allowed.")
            false
        }
    }

    override fun sendLocation(location: Location, isAlarm: Boolean, cache: Boolean): Boolean {
        return if (communicationContext.isMessageAllowed(this.javaClass.name, if (isAlarm) MessageType.AlarmLocation.name else MessageType.Location.name, "send")) {
            Log.d(tag, "Sending SMS message with actual device location.")
            sendMessage(communicationContext.appContext.getString(R.string.sms_location, location.latitude.toString(), location.longitude.toString()))
        } else {
            Log.d(tag, "SMS message with location is not allowed.")
            false
        }
    }

    override fun sendStatus(battery: Float, isCharging: Boolean, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>): Boolean {
        return if (communicationContext.isMessageAllowed(this.javaClass.name, MessageType.Status.name, "recv")) {
            Log.d(tag, "Sending status SMS message.")

            val batteryInfoResource =
                    if (isCharging) R.string.sms_info_battery_charging
                    else R.string.sms_info_battery_not_charging
            val batteryInfo = communicationContext.appContext.getString(batteryInfoResource, battery.toString())

            val powerSaveModeResource =
                    if (powerSaveMode) R.string.sms_info_power_save_mode_on
                    else R.string.sms_info_power_save_mode_off
            val powerSaveModeInfo = communicationContext.appContext.getString(powerSaveModeResource)

            var utilsInfo = ""
            for (util in utils.keys) {

                val utilResource = if (utils[util] == true) R.string.sms_util_enabled else R.string.sms_util_disabled
                utilsInfo += "\n"
                utilsInfo += communicationContext.appContext.getString(utilResource, util.name)
            }

            sendMessage(batteryInfo + "\n" + powerSaveModeInfo + utilsInfo)
        } else {
            Log.d(tag, "Status SMS message is not allowed.")
            false
        }
    }

    override fun sendRoute(localRouteId: Int): Boolean {
        // Method is not implemented
        return true
    }
}
