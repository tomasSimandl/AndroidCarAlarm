package com.example.tomas.carsecurity.communication.network

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.support.v4.content.ContextCompat
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjByte
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.communication.ICommunicationProvider
import com.example.tomas.carsecurity.communication.MessageType
import com.example.tomas.carsecurity.communication.network.controller.EventController
import com.example.tomas.carsecurity.communication.network.controller.RouteController
import com.example.tomas.carsecurity.communication.network.dto.EventCreate
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.utils.UtilsEnum
import java.util.*

class NetworkProvider(private val communicationContext: CommunicationContext) : ICommunicationProvider,  SharedPreferences.OnSharedPreferenceChangeListener {

    private val tag = "NetworkProvider"
    private lateinit var workerThread: WorkerThread
    private lateinit var routeController: RouteController
    private lateinit var eventController: EventController

    companion object Check : CheckObjByte {
        override fun check(context: Context): Byte {
            return if (ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.d("NetworkProvider.check", "permission denied")
                CheckCodes.permissionDenied
            } else if (CommunicationContext(context).serverUrl.isBlank()) {
                Log.d("NetworkProvider.check", "invalid parameters")
                CheckCodes.invalidParameters
            } else if (!CommunicationContext(context).isProviderAllowed(NetworkProvider::class.java.name)) {
                Log.d("NetworkProvider.check", "not allowed")
                CheckCodes.notAllowed
            } else {
                Log.d("NetworkProvider.check", "success")
                CheckCodes.success
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {

        if (key == communicationContext.appContext.getString(R.string.key_communication_network_server_url)) {
            Log.d(tag, "Servers url was changed. Reinitializing Controllers")
            initControllers()
        }
    }

    override fun initialize(): Boolean {
        if (check(communicationContext.appContext) == CheckCodes.success) {

            initControllers()

            workerThread = WorkerThread("NetworkThread")
            workerThread.start()
            workerThread.prepareHandler()

            communicationContext.registerOnPreferenceChanged(this)
            return true
        }
        return false
    }

    private fun initControllers(){
        routeController = RouteController(communicationContext.serverUrl)
        eventController = EventController(communicationContext.serverUrl)
    }

    override fun destroy() {
        if (::workerThread.isInitialized) workerThread.quit()
        communicationContext.unregisterOnPreferenceChanged(this)
    }

    override fun isInitialize(): Boolean {
        return ::workerThread.isInitialized
    }

    private fun canSendMessage(): Boolean {
        if (check(communicationContext.appContext) != CheckCodes.success) {
            Log.d(tag, "Can not send message to server.")
            return false
        }
        return true
    }

    override fun sendUtilSwitch(utilsEnum: UtilsEnum, enabled: Boolean): Boolean {
        if (!canSendMessage()) return false

        return if (communicationContext.isMessageAllowed(this.javaClass.name, "Tool_State_Changed_send")) {
            Log.d(tag, "Sending util switch network message of util: ${utilsEnum.name}.")
            // TODO (Implement)
            true
        } else {
            Log.d(tag, "Util switch Network message is not allowed for util ${utilsEnum.name}.")
            // TODO (Implement)
            false
        }
    }

    private fun getEventType(messageType: MessageType): Int {
        return when (messageType) {
            MessageType.UtilSwitch ->
                communicationContext.appContext.resources.getInteger(R.integer.event_unknown)
            MessageType.Alarm ->
                communicationContext.appContext.resources.getInteger(R.integer.event_alarm_on)
            MessageType.AlarmLocation ->
                communicationContext.appContext.resources.getInteger(R.integer.event_unknown)
            MessageType.Location ->
                communicationContext.appContext.resources.getInteger(R.integer.event_unknown)
            MessageType.BatteryWarn ->
                communicationContext.appContext.resources.getInteger(R.integer.event_battery)
            MessageType.Status ->
                communicationContext.appContext.resources.getInteger(R.integer.event_unknown)
            MessageType.PowerConnected ->
                communicationContext.appContext.resources.getInteger(R.integer.event_power_connected)
            MessageType.PowerDisconnected ->
                communicationContext.appContext.resources.getInteger(R.integer.event_power_disconnected)

        }
    }

    private fun canSendEvent(messageType: MessageType): Boolean {
        return when (messageType) {

            MessageType.Alarm ->
                communicationContext.isMessageAllowed(this.javaClass.name, "Alarm_Position_send")
            MessageType.BatteryWarn,
            MessageType.PowerConnected,
            MessageType.PowerDisconnected ->
                communicationContext.isMessageAllowed(this.javaClass.name, "Battery_State_Changed_send")
            else -> {
                Log.e(tag, "Sending Network message of message type $messageType is not supported as Event sending.")
                false
            }

        }
    }

    override fun sendEvent(messageType: MessageType, vararg args: Any): Boolean {
        if (!canSendMessage()) return false
        if (!canSendEvent(messageType)) return false

        // TODO (args are not added to event)

        val event = EventCreate(getEventType(messageType).toLong(), Calendar.getInstance().time, 1, "") // TODO (use real car id)
        val task = Runnable {
            eventController.createEvent(event)
        }

        workerThread.postTask(task)
        return ::workerThread.isInitialized
    }

    override fun sendLocation(location: Location, isAlarm: Boolean, cache: Boolean): Boolean {
        if (!canSendMessage()) return false
        if (isAlarm && !communicationContext.isMessageAllowed(this.javaClass.name, "Alarm_Position_send")) {
            Log.d(tag, "Sending alarm location network message is not allowed.")
            return false
        }

        return if (cache) {
            // TODO store to DB
            true
        } else {
            // locationController.createLocation(location)
            true
        }
    }

    override fun sendStatus(battery: Int, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>): Boolean {
        if (!canSendMessage()) return false
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}