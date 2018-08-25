package com.example.tomas.carsecurity.communication

import android.location.Location
import android.telephony.SmsManager
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.UtilsEnum
import com.google.android.gms.common.util.Strings
import java.util.*

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

        return if(context.communicationContext.canSendMessage(this.javaClass.name, utilsEnum.name, MessageType.UtilSwitch.name)){

            val text = if(enabled)
                context.appContext.getString(R.string.sms_util_enabled, utilsEnum.name)
            else
                context.appContext.getString(R.string.sms_util_disabled, utilsEnum.name)

            sendMessage(text)

        } else {
            false
        }
    }

    override fun sendAlarm(): Boolean{

        return if(context.communicationContext.canSendMessage(this.javaClass.name, MessageType.Alarm.name)){
            sendMessage(context.appContext.getString(R.string.sms_alarm, Calendar.getInstance().time.toString()))
        } else {
            false
        }
    }

    override fun sendLocation(location: Location): Boolean {
        return if(context.communicationContext.canSendMessage(this.javaClass.name, MessageType.Location.name)){
            sendMessage(context.appContext.getString(R.string.sms_location, location.latitude.toString(), location.longitude.toString()))
        } else {
            false
        }
    }

    override fun sendBatteryWarn(capacity: Int): Boolean {
        return if(context.communicationContext.canSendMessage(this.javaClass.name, MessageType.BatteryWarn.name)){
            sendMessage(context.appContext.getString(R.string.sms_battery_warn, capacity))
        } else {
            false
        }
    }

    override fun sendStatus(battery: Int, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>): Boolean {
        return if(context.communicationContext.canSendMessage(this.javaClass.name, MessageType.Status.name)){

            val batteryInfo = context.appContext.getString(R.string.sms_info_battery, battery)
            val powerSaveModeResource = if(powerSaveMode) R.string.sms_info_power_save_mode_on else R.string.sms_info_power_save_mode_off
            val powerSaveModeInfo = context.appContext.getString(powerSaveModeResource)
            var utilsInfo = ""
            for(util in utils.keys){

                val utilResource = if(utils[util] == true) R.string.sms_util_enabled else R.string.sms_util_disabled

                utilsInfo += "\n"
                utilsInfo += context.appContext.getString(utilResource, util.name)
            }

            sendMessage(batteryInfo + "\n" + powerSaveModeInfo + utilsInfo)
        } else {
            false
        }
    }
}
