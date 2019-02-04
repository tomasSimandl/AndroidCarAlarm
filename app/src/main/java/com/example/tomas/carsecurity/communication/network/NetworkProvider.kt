package com.example.tomas.carsecurity.communication.network

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.storage.entity.Message
import com.example.tomas.carsecurity.storage.entity.Route
import com.example.tomas.carsecurity.utils.UtilsEnum
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.util.*
import kotlin.collections.ArrayList


class NetworkProvider(private val communicationContext: CommunicationContext) : ICommunicationProvider, SharedPreferences.OnSharedPreferenceChangeListener, BroadcastReceiver() {

    private val tag = "NetworkProvider"
    private lateinit var workerThread: WorkerThread
    private lateinit var routeController: RouteController
    private lateinit var eventController: EventController
    private lateinit var locationController: LocationController

    private val connectivityService = communicationContext.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

    private var isInitialized: Boolean = false

    private var synchronizeTimer: Timer? = null

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

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            onNetworkStatusChanged()
        }
    }

    private fun onNetworkStatusChanged(){
        synchronized(this) {
            if (canUseConnection()) {
                Log.d(tag, "Network connectivity was changed and it is possible to send data.")

                if (synchronizeTimer == null) {
                    val synchronizeTask = object : TimerTask() {
                        override fun run() {
                            if (!canUseConnection()) {
                                Log.d(tag, "Stopping network synchronization. Can not use connection.")
                                return
                            }

                            val storage = StorageService.getInstance(communicationContext.appContext)

                            val messages = storage.getMessages(NetworkProvider.hashCode())
                            for (message in messages) {
                                sendEvent(message)
                            }

                            val routes = storage.getRoutes()
                            val routesWithId: MutableList<Route> = ArrayList()
                            var maxRouteId: Int = Integer.MIN_VALUE
                            for (route in routes) {
                                if (route.uid > maxRouteId) maxRouteId = route.uid

                                if (route.serverRouteId != null) {
                                    routesWithId.add(route)
                                } else {
                                    sendRoute(route)
                                }
                            }

                            for (route in routesWithId) {
                                val locations = storage.getLocationsByLocalRouteId(route.uid)
                                for (location in locations) {
                                    sendLocation(location)
                                }

                                // Can not remove last route because it is possibility that is stil used
                                if (route.uid < maxRouteId && storage.getLocationsByLocalRouteId(route.uid).isEmpty()) {
                                    storage.deleteRoute(route)
                                }
                            }

                            val locations = storage.getLocationsByLocalRouteId(null)
                            for (location in locations) {
                                sendLocation(location)
                            }
                        }
                    }
                    synchronizeTimer = Timer("NetworkSynchronize")
                    synchronizeTimer!!.schedule(synchronizeTask, 1000L, communicationContext.synchronizationInterval)
                }
            } else {
                Log.d(tag, "Network connectivity was changed. Can not send data.")

                if (synchronizeTimer != null) {
                    synchronizeTimer!!.cancel()
                    synchronizeTimer = null
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {

        when (key) {
            communicationContext.appContext.getString(R.string.key_communication_network_server_url) -> {
                Log.d(tag, "Servers url was changed. Reinitializing Controllers")
                if (!initControllers()) {
                    isInitialized = false
                }
            }

            communicationContext.appContext.getString(R.string.key_communication_network_cellular) -> {
                Log.d(tag, "Cellular status changed")
                onNetworkStatusChanged()
            }

            communicationContext.appContext.getString(R.string.key_communication_network_update_interval) -> {
                Log.d(tag, "Update interval was changed")
                synchronized(this) {
                    if (synchronizeTimer != null) {
                        synchronizeTimer!!.cancel()
                        synchronizeTimer = null
                    }
                }
                onNetworkStatusChanged()
            }
        }
    }

    override fun initialize(): Boolean {
        isInitialized = false

        if (check(communicationContext.appContext) == CheckCodes.success) {

            Log.d(tag, "Initializing Network provider")
            if (!initControllers()) return false

            workerThread = WorkerThread("NetworkThread")
            workerThread.start()
            workerThread.prepareHandler()

            communicationContext.registerOnPreferenceChanged(this)

            val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
            communicationContext.appContext.registerReceiver(this, intentFilter)

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
        Log.d(tag, "Destroying")
        isInitialized = false
        if (::workerThread.isInitialized) workerThread.quit()
        communicationContext.unregisterOnPreferenceChanged(this)
        communicationContext.appContext.unregisterReceiver(this)

        if(synchronizeTimer != null){
            synchronizeTimer!!.cancel()
            synchronizeTimer = null
        }
    }

    override fun isInitialize(): Boolean {
        return isInitialized
    }

    override fun sendUtilSwitch(utilsEnum: UtilsEnum, enabled: Boolean): Boolean {
        val actualTime = Calendar.getInstance().timeInMillis

        val task = Runnable {
            if (!canSendMessage()) return@Runnable

            if (communicationContext.isMessageAllowed(this.javaClass.name, "Tool_State_Changed_send")) {
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

                val event = EventCreate(messageType, actualTime, 1, note) // TODO (use real car id)
                val eventStr = Gson().toJson(event)
                val message = Message(communicatorHash = NetworkProvider.hashCode(), message = eventStr)

                sendEvent(message)
            } else {
                Log.d(tag, "Util switch Network message is not allowed for util ${utilsEnum.name}.")
            }
        }
        workerThread.postTask(task)
        return true
    }

    override fun sendEvent(messageType: MessageType, vararg args: String): Boolean {
        val actualTime = Calendar.getInstance().timeInMillis
        val task = Runnable {
            if (!canSendMessage()) return@Runnable
            if (!canSendEvent(messageType)) return@Runnable

            var note = ""
            args.iterator().forEach { item -> note += "$item," }
            if (note.isNotEmpty()) note = note.dropLast(1)

            val event = EventCreate(getEventType(messageType).toLong(), actualTime, 1, note) // TODO (use real car id)
            val eventStr = Gson().toJson(event)
            val message = Message(communicatorHash = NetworkProvider.hashCode(), message = eventStr)

            sendEvent(message)
        }
        workerThread.postTask(task)

        return true
    }


    override fun sendLocation(location: Location, isAlarm: Boolean, cache: Boolean): Boolean {

        val task = Runnable {
            if (!canSendMessage()) return@Runnable
            if (isAlarm && !communicationContext.isMessageAllowed(this.javaClass.name, "Alarm_Position_send")) {
                Log.d(tag, "Sending alarm location network message is not allowed.")
                return@Runnable
            }

            if (cache) {
                StorageService.getInstance(communicationContext.appContext).saveLocation(location)
            } else {
                sendLocation(location)
            }
        }
        workerThread.postTask(task)
        return true
    }

    override fun sendRoute(localRouteId: Int): Boolean {
        val task = Runnable {
            if (!canSendMessage()) return@Runnable

            val route = StorageService.getInstance(communicationContext.appContext).getRoute(localRouteId)
            sendRoute(route)
        }

        workerThread.postTask(task)
        return true
    }

    override fun sendStatus(battery: Int, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>): Boolean {
        if (!canSendMessage()) return false
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Can not run in main thread
     */
    private fun sendEvent(msg: Message) {

        var inDB = true

        if (canUseConnection()) {
            val result = eventController.createEvent(msg.message)
            if (result.isSuccessful) {
                inDB = false
            }
        }

        if (inDB && msg.uid == 0) {
            // should be in DB but msg.uid == 0 => it is not in DB
            StorageService.getInstance(communicationContext.appContext).saveMessage(msg)
        } else if (!inDB && msg.uid != 0) {
            // should not be in DB but msg.uid != 0 => it is in DB
            StorageService.getInstance(communicationContext.appContext).deleteMessage(msg)
        }
    }

    /**
     * Can not run in main thread
     */
    private fun sendLocation(location: Location) {

        var inDB = true
        var update = false
        var send = true
        val storage = StorageService.getInstance(communicationContext.appContext)

        if (canUseConnection()) {

            if (location.localRouteId != null && location.routeId == null) {
                val route = storage.getRoute(location.localRouteId!!)
                if (route.serverRouteId == null) {
                    Log.w(tag, "Can not send position because route was not created yet.")
                    inDB = true
                    send = false
                } else {
                    location.routeId = route.serverRouteId
                    update = true
                }
            }

            if (send) {
                val result = locationController.createLocations(listOf(location))
                if (result.isSuccessful) inDB = false
                if (result.code() == HttpURLConnection.HTTP_CONFLICT) inDB = false // already in DB
            }
        }

        if (inDB && location.uid != 0 && update) {
            // should be in DB it is in DB but need to be updated
            storage.updateLocation(location)
        } else if (inDB && location.uid == 0) {
            // should be in DB but location.uid == 0 => it is not in DB
            storage.saveLocation(location)
        } else if (!inDB && location.uid != 0) {
            // should not be in DB but location.uid != 0 => it is in DB
            storage.deleteLocations(listOf(location))
        }
    }

    /**
     * Can not run in main thread
     */
    private fun sendRoute(route: Route) {

        if (route.serverRouteId != null) {
            Log.e(tag, "Route is already created.")
            return
        }

        if (canUseConnection()) {
            val storage = StorageService.getInstance(communicationContext.appContext)

            val response = routeController.createRoute(route.carId)
            if (response.isSuccessful) {
                val jsonResponse = JsonParser().parse(response.body().toString()).asJsonObject
                val serverRouteId = jsonResponse["route_id"].asLong

                route.serverRouteId = serverRouteId
                if (route.uid == 0) {
                    storage.saveRoute(route)
                } else {
                    storage.updateRoute(route)
                }

                Log.d(tag, "Route was successfully created and stored to DB")
            } else {
                Log.d(tag, "Creating of route was not successful")
            }
        }

        if (route.uid == 0) {
            StorageService.getInstance(communicationContext.appContext).saveRoute(route)
        }
    }

    private fun isConnected(): Boolean {
        return connectivityService?.activeNetworkInfo?.isConnected ?: false
    }

    private fun isCellular(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            connectivityService?.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
        } else {
            val activeNetwork = connectivityService?.activeNetwork ?: return false

            val capabilities = connectivityService.getNetworkCapabilities(activeNetwork)
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }
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

    private fun canUseConnection(): Boolean {
        return isConnected() && (communicationContext.cellular || !isCellular())
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
}