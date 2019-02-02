package com.example.tomas.carsecurity.communication.network

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjByte
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.communication.ICommunicationProvider
import com.example.tomas.carsecurity.communication.MessageType
import com.example.tomas.carsecurity.communication.network.controller.EventController
import com.example.tomas.carsecurity.communication.network.controller.LocationController
import com.example.tomas.carsecurity.communication.network.controller.RouteController
import com.example.tomas.carsecurity.communication.network.dto.EventCreate
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.storage.StorageService
import com.example.tomas.carsecurity.storage.entity.Message
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.utils.UtilsEnum
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.util.*

class NetworkProvider(private val communicationContext: CommunicationContext) : ICommunicationProvider, SharedPreferences.OnSharedPreferenceChangeListener {

    private val tag = "NetworkProvider"
    private lateinit var workerThread: WorkerThread
    private lateinit var routeController: RouteController
    private lateinit var eventController: EventController
    private lateinit var locationController: LocationController

    private var isInitialized: Boolean = false

    companion object Check : CheckObjByte {
        override fun check(context: Context): Byte {
            val communicationContext = CommunicationContext(context)

            return if (ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.d("NetworkProvider.check", "permission denied")
                CheckCodes.permissionDenied
            } else if (communicationContext.serverUrl.isBlank() ||
                    (!communicationContext.serverUrl.startsWith("http://") && !communicationContext.serverUrl.startsWith("https://")) ||
                    !communicationContext.serverUrl.endsWith("/")) {
                Log.d("NetworkProvider.check", "invalid parameters")
                CheckCodes.invalidParameters
            } else if (!communicationContext.isProviderAllowed(NetworkProvider::class.java.name)) {
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
            if (!initControllers()) {
                isInitialized = false
            }
        }
    }

    override fun initialize(): Boolean {
        isInitialized = false

        if (check(communicationContext.appContext) == CheckCodes.success) {

            if (!initControllers()) return false

            workerThread = WorkerThread("NetworkThread")
            workerThread.start()
            workerThread.prepareHandler()

            communicationContext.registerOnPreferenceChanged(this)
            isInitialized = true
            return true
        }
        return false
    }

    private fun initControllers(): Boolean {
        return try {
            routeController = RouteController(communicationContext.serverUrl)
            eventController = EventController(communicationContext.serverUrl)
            locationController = LocationController(communicationContext.serverUrl)
            true
        } catch (e: Exception) {
            Log.e(tag, "Can not initialize Controllers: $e")
            false
        }
    }

    override fun destroy() {
        isInitialized = false
        if (::workerThread.isInitialized) workerThread.quit()
        communicationContext.unregisterOnPreferenceChanged(this)
    }

    override fun isInitialize(): Boolean {
        return isInitialized
    }

    private fun canSendMessage(): Boolean {
        if (!isInitialized) {
            Log.w(tag, "Can not send message because NetworkProvider is not initialized.")
            return false
        }

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

            val messageType: Long
            val note: String
            if (enabled) {
                messageType = communicationContext.appContext.resources.getInteger(R.integer.event_util_switch_on).toLong()
                note = "Util ${utilsEnum.name} was turned on."
            } else {
                messageType = communicationContext.appContext.resources.getInteger(R.integer.event_util_switch_off).toLong()
                note = "Util ${utilsEnum.name} was turned off."
            }
            createEvent(messageType, 1, note) // TODO (use real car id)

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

    override fun sendEvent(messageType: MessageType, vararg args: String): Boolean {
        if (!canSendMessage()) return false
        if (!canSendEvent(messageType)) return false

        var note = ""
        args.iterator().forEach { item -> note += "$item," }
        if (note.isNotEmpty()) note = note.dropLast(1)

        createEvent(getEventType(messageType).toLong(), 1, note) // TODO (use real car id)

        return true
    }

    private fun createEvent(eventType: Long, carId: Long, note: String) {
        val event = EventCreate(eventType, Calendar.getInstance().time, carId, note)

        val task = Runnable {
            val strEvent = Gson().toJson(event)
            val result = eventController.createEvent(strEvent)
            if (!result.isSuccessful) {
                StorageService.getInstance(communicationContext.appContext).saveMessage(Message(communicatorHash = NetworkProvider.hashCode(), message = strEvent))
                // TODO (maybe set timeout to load from db)
            }
        }

        workerThread.postTask(task)
    }

    override fun sendLocation(location: Location, isAlarm: Boolean, cache: Boolean): Boolean {
        if (!canSendMessage()) return false
        if (isAlarm && !communicationContext.isMessageAllowed(this.javaClass.name, "Alarm_Position_send")) {
            Log.d(tag, "Sending alarm location network message is not allowed.")
            return false
        }

        return if (cache) {
            StorageService.getInstance(communicationContext.appContext).saveLocation(location)
            true
        } else {
            val task = Runnable {
                if (location.localRouteId != null){
                    val storage = StorageService.getInstance(communicationContext.appContext)
                    val route = storage.getRoute(location.localRouteId!!)
                    if(route.serverRouteId == null){
                        Log.w(tag, "Can not send position because route was not created yet.")
                        return@Runnable
                    }
                    location.routeId = route.serverRouteId
                }
                val result = locationController.createLocations(listOf(location))
                if (!result.isSuccessful) {
                    StorageService.getInstance(communicationContext.appContext).saveLocation(location)
                    // TODO (maybe set timeout to load from db)
                }
            }
            workerThread.postTask(task)
            true
        }
    }

    override fun sendRoute(localRouteId: Int): Boolean {
        if (!canSendMessage()) return false

        val storage = StorageService.getInstance(communicationContext.appContext)
        val route = storage.getRoute(localRouteId)

        if (route.serverRouteId != null) {
            Log.e(tag, "Route is already created.")
            return false
        }

        val task = Runnable {

            val response = routeController.createRoute(route.carId)
            if (response.isSuccessful) {
                val jsonResponse = JsonParser().parse(response.body().toString()).asJsonObject
                val serverRouteId = jsonResponse["route_id"].asLong

                route.serverRouteId = serverRouteId
                storage.updateRoute(route)
                Log.d(tag, "Route was successfully created and stored to DB")
                // TODO (inform something that positions can be send to server now)
            } else {
                Log.d(tag, "Creating of route was not successful")
            }
        }

        workerThread.postTask(task)
        return true
    }

    override fun sendStatus(battery: Int, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>): Boolean {
        if (!canSendMessage()) return false
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}