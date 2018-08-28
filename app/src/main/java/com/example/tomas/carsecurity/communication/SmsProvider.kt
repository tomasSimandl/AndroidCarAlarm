package com.example.tomas.carsecurity.communication

import android.location.Location
import android.telephony.SmsManager
import android.util.Log
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.utils.UtilsEnum
import com.google.android.gms.common.util.Strings
import java.util.*

class SmsProvider(private val communicationContext: CommunicationContext) : ICommunicationProvider {
    private val tag = "SmsProvider"
    
    private val smsManager = SmsManager.getDefault()

    override fun sendMessage(text: String): Boolean {
        if(Strings.isEmptyOrWhitespace(communicationContext.phoneNumber) || Strings.isEmptyOrWhitespace(text)){
            Log.d(tag, "Empty phone number or text of message is empty")
            return false
        }

        val textParts = smsManager.divideMessage(text)

        if (textParts.size > 1) {
            Log.d(tag, "Sending multipart message.")
            smsManager.sendMultipartTextMessage(communicationContext.phoneNumber,null, textParts, null, null)
        } else {
            Log.d(tag, "Sending message.")
            smsManager.sendTextMessage(communicationContext.phoneNumber, null, text, null, null)
        }
        return true
    }

    override fun sendUtilSwitch(utilsEnum: UtilsEnum, enabled: Boolean): Boolean {

        return if(communicationContext.canSendMessage(this.javaClass.name, utilsEnum.name, MessageType.UtilSwitch.name)){

            val text = if(enabled)
                communicationContext.context.getString(R.string.sms_util_enabled, utilsEnum.name)
            else
                communicationContext.context.getString(R.string.sms_util_disabled, utilsEnum.name)

            Log.d(tag, """Sending sms util switch message of util: ${utilsEnum.name}""")
            sendMessage(text)

        } else {
            Log.d(tag, """Sms util switch message is not allowed for util: ${utilsEnum.name}""")
            false
        }
    }

    override fun sendAlarm(): Boolean{

        return if(communicationContext.canSendMessage(this.javaClass.name, MessageType.Alarm.name)){
            Log.d(tag, "Sending alarm sms message.")
            sendMessage(communicationContext.context.getString(R.string.sms_alarm, Calendar.getInstance().time.toString()))
        } else {
            Log.d(tag, "Sms message for alarm is not allowed.")
            false
        }
    }

    override fun sendLocation(location: Location): Boolean {
        return if(communicationContext.canSendMessage(this.javaClass.name, MessageType.Location.name)){
            Log.d(tag, "Sending sms message with actual device location.")
            sendMessage(communicationContext.context.getString(R.string.sms_location, location.latitude.toString(), location.longitude.toString()))
        } else {
            Log.d(tag, "Sms message with location is not allowed.")
            false
        }
    }

    override fun sendBatteryWarn(capacity: Int): Boolean {
        return if(communicationContext.canSendMessage(this.javaClass.name, MessageType.BatteryWarn.name)){
            Log.d(tag, "Sending battery warning sms message.")
            sendMessage(communicationContext.context.getString(R.string.sms_battery_warn, capacity))
        } else {
            Log.d(tag, "Battery warning sms message is not allowed.")
            false
        }
    }

    override fun sendStatus(battery: Int, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>): Boolean {
        return if(communicationContext.canSendMessage(this.javaClass.name, MessageType.Status.name)){

            val batteryInfo = communicationContext.context.getString(R.string.sms_info_battery, battery)
            val powerSaveModeResource = if(powerSaveMode) R.string.sms_info_power_save_mode_on else R.string.sms_info_power_save_mode_off
            val powerSaveModeInfo = communicationContext.context.getString(powerSaveModeResource)
            var utilsInfo = ""
            for(util in utils.keys){

                val utilResource = if(utils[util] == true) R.string.sms_util_enabled else R.string.sms_util_disabled

                utilsInfo += "\n"
                utilsInfo += communicationContext.context.getString(utilResource, util.name)
            }

            Log.d(tag, "Sending sms status message.")
            sendMessage(batteryInfo + "\n" + powerSaveModeInfo + utilsInfo)
        } else {
            Log.d(tag, "Status sms message is not allowed.")
            false
        }
    }
}
