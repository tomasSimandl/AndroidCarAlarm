package com.example.tomas.carsecurity.communication.sms

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjByte
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.ICommunicationProvider
import com.example.tomas.carsecurity.communication.MessageType
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.tools.ToolsEnum
import com.google.android.gms.common.util.Strings
import java.util.*

/**
 * Class is used for sending SMS messages to phone number which is taken from SharedPreferences.
 */
class SmsProvider(private val communicationContext: CommunicationContext) : ICommunicationProvider {

    /** Logger tag */
    private val tag = "SmsProvider"

    /** Sms Manager used for sending SMS messages */
    private lateinit var smsManager: SmsManager
    /** Sms Broadcast receiver used for receiving and handling SMS messages */
    private lateinit var smsBroadcastReceiver: SmsBroadcastReceiver

    /** Indication if provider is successfully initialized. */
    private var isInitialize = false

    /**
     * Object is used for check if SmsProvider can be initialized.
     */
    companion object Check : CheckObjByte {
        override fun check(context: Context): Byte {
            return if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                CheckCodes.hardwareNotSupported
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                CheckCodes.permissionDenied
            } else if (!PhoneNumberUtils.isGlobalPhoneNumber(CommunicationContext(context).phoneNumber)) {
                CheckCodes.invalidParameters
            } else if (!CommunicationContext(context).isProviderAllowed(SmsProvider::class.java.name)) {
                CheckCodes.notAllowed
            } else {
                CheckCodes.success
            }
        }
    }

    /**
     * Method for initialization of SmsProvider. On success initialization method return true.
     *
     * @return true on success, false otherwise
     */
    override fun initialize(): Boolean {
        Log.d(tag, "init")
        if (check(communicationContext.appContext) == CheckCodes.success) {

            smsManager = SmsManager.getDefault()
            smsBroadcastReceiver = SmsBroadcastReceiver(communicationContext)

            val intentFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
            communicationContext.appContext.registerReceiver(smsBroadcastReceiver, intentFilter)
            isInitialize = true
            Log.d(tag, "init success")
            return true
        }
        Log.d(tag, "init failed")
        isInitialize = false
        return false
    }

    /**
     * Method destroy all initialized data in SmsProvider. This method should be called before this instance is destroy.
     */
    override fun destroy() {
        Log.d(tag, "destroy")
        if (isInitialize) {
            communicationContext.appContext.unregisterReceiver(smsBroadcastReceiver)
            isInitialize = false
        }
    }

    /**
     * Return if SmsProvider is successfully initialized.
     *
     * @return if SmsProvider is successfully initialized.
     */
    override fun isInitialize(): Boolean {
        return isInitialize
    }

    /**
     * Method send input text over SMS to phone number specified in SharedPreferences. If message is too long, it is
     * divided to smaller messages.
     *
     * @param text of SMS which should be send
     * @return true when message was successfully send to SmsManager, false otherwise.
     */
    private fun sendMessage(text: String): Boolean {
        if (check(communicationContext.appContext) != CheckCodes.success) {
            Log.d(tag, "Can not send SMS. Permission not granted or unsupported hardware")
            return false
        }

        if (!PhoneNumberUtils.isGlobalPhoneNumber(communicationContext.phoneNumber) || Strings.isEmptyOrWhitespace(text)) {
            Log.d(tag, "Invalid phone number or text of message is empty")
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

    /**
     * Send information about activation or deactivation of util. Text of SMS is from resources.
     *
     * @param toolsEnum enum which identifies util which was changed
     * @param enabled indicates if util was activate - true or deactivate - false
     * @return true when message was successfully send to SmsManager, false otherwise.
     */
    override fun sendUtilSwitch(toolsEnum: ToolsEnum, enabled: Boolean): Boolean {

        return if (communicationContext.isMessageAllowed(this.javaClass.name, toolsEnum.name, MessageType.UtilSwitch.name, "send")) {
            Log.d(tag, "Sending util switch SMS message of util: ${toolsEnum.name}.")

            val text = if (enabled)
                communicationContext.appContext.getString(R.string.sms_util_enabled, toolsEnum.name)
            else
                communicationContext.appContext.getString(R.string.sms_util_disabled, toolsEnum.name)

            Log.d(tag, """Sending sms util switch message of util: ${toolsEnum.name}""")
            sendMessage(text)

        } else {
            Log.d(tag, "Util switch SMS message is not allowed for util ${toolsEnum.name}.")
            false
        }
    }

    /**
     * Send event given by message type and message arguments to server.
     *
     * @param messageType is type of event message which should be send over SMS.
     * @param args are arguments to message which is taken from resources.
     * @return true when message was successfully send to SmsManager, false otherwise.
     */
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

    /**
     * Send SMS indicates that alarm was activated.
     *
     * @return true when message was successfully send to SmsManager, false otherwise.
     */
    private fun sendAlarm(): Boolean{
        return if(communicationContext.isMessageAllowed(this.javaClass.name, MessageType.Alarm.name, "send")){
            Log.d(tag, "Sending alarm sms message.")
            sendMessage(communicationContext.appContext.getString(R.string.sms_alarm, Calendar.getInstance().time.toString()))
        } else {
            Log.d(tag, "Sms message for alarm is not allowed.")
            false
        }
    }

    /**
     * Send SMS with actual battery capacity.
     *
     * @param capacity percents of actual battery capacity
     * @return true when message was successfully send to SmsManager, false otherwise.
     */
    private fun sendBatteryWarn(capacity: String): Boolean {
        return if(communicationContext.isMessageAllowed(this.javaClass.name, MessageType.BatteryWarn.name, "send")){
            Log.d(tag, "Sending battery warning sms message.")
            sendMessage(communicationContext.appContext.getString(R.string.sms_battery_warn, capacity))
        } else {
            Log.d(tag, "Battery warning sms message is not allowed.")
            false
        }
    }

    /**
     * Send information message that device was connected to external source of power.
     *
     * @param capacity percents of actual battery capacity
     * @return true when message was successfully send to SmsManager, false otherwise.
     */
    private fun sendPowerConnected(capacity: String): Boolean {
        return if(communicationContext.isMessageAllowed(this.javaClass.name, MessageType.PowerConnected.name, "send")){
            Log.d(tag, "Sending power connected warning sms message.")
            sendMessage(communicationContext.appContext.getString(R.string.sms_power_connected, capacity))
        } else {
            Log.d(tag, "Power connected warning sms message is not allowed.")
            false
        }
    }

    /**
     * Send information message that device was disconnected from external source of power.
     *
     * @param capacity percents of actual battery capacity
     * @return true when message was successfully send to SmsManager, false otherwise.
     */
    private fun sendPowerDisconnected(capacity: String): Boolean {
        return if(communicationContext.isMessageAllowed(this.javaClass.name, MessageType.PowerDisconnected.name, "send")){
            Log.d(tag, "Sending power disconnected warning sms message.")
            sendMessage(communicationContext.appContext.getString(R.string.sms_power_disconnected, capacity))
        } else {
            Log.d(tag, "Power disconnected warning sms message is not allowed.")
            false
        }
    }

    /**
     * Send SMS with input location.
     *
     * @param location actual device location which will be sent
     * @param isAlarm indication if device is in alarm mode
     * @param cache not used in this implementation
     * @return true when message was successfully send to SmsManager, false otherwise.
     */
    override fun sendLocation(location: Location, isAlarm: Boolean, cache: Boolean): Boolean {
        return if (communicationContext.isMessageAllowed(this.javaClass.name, if (isAlarm) MessageType.AlarmLocation.name else MessageType.Location.name, "send")) {
            Log.d(tag, "Sending SMS message with actual device location.")
            sendMessage(communicationContext.appContext.getString(R.string.sms_location, location.latitude.toString(), location.longitude.toString()))
        } else {
            Log.d(tag, "SMS message with location is not allowed.")
            false
        }
    }

    /**
     * Send actual status with SMS composed from input parameters.
     *
     * @param battery percentage status of battery level
     * @param isCharging indication if device is connected to external source of power
     * @param tools list of activated tools
     * @return true when message was successfully send to SmsManager, false otherwise.
     */
    override fun sendStatus(battery: Float, isCharging: Boolean, powerSaveMode: Boolean, tools: Map<ToolsEnum, Boolean>): Boolean {
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
            for (util in tools.keys) {

                val utilResource = if (tools[util] == true) R.string.sms_util_enabled else R.string.sms_util_disabled
                utilsInfo += "\n"
                utilsInfo += communicationContext.appContext.getString(utilResource, util.name)
            }

            sendMessage(batteryInfo + "\n" + powerSaveModeInfo + utilsInfo)
        } else {
            Log.d(tag, "Status SMS message is not allowed.")
            false
        }
    }

    /**
     * Method is not implemented in this version of implementation.
     */
    override fun sendRoute(localRouteId: Int): Boolean {
        // Method is not implemented
        return true
    }
}
