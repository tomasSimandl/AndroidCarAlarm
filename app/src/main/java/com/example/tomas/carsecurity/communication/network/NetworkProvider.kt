package com.example.tomas.carsecurity.communication.network

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjByte
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.communication.ICommunicationProvider
import com.example.tomas.carsecurity.communication.MessageType
import com.example.tomas.carsecurity.communication.network.controller.*
import com.example.tomas.carsecurity.communication.network.dto.EventCreate
import com.example.tomas.carsecurity.communication.network.dto.StatusCreate
import com.example.tomas.carsecurity.communication.network.dto.Token
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.fragments.LoginFragment
import com.example.tomas.carsecurity.storage.Storage
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.storage.entity.Message
import com.example.tomas.carsecurity.storage.entity.Route
import com.example.tomas.carsecurity.storage.entity.User
import com.example.tomas.carsecurity.utils.UtilsEnum
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.provider.FirebaseInitProvider
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.internal.LinkedTreeMap
import okhttp3.OkHttpClient
import java.io.Serializable
import java.net.HttpURLConnection
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList


class NetworkProvider (private val communicationContext: CommunicationContext) :
        ICommunicationProvider, SharedPreferences.OnSharedPreferenceChangeListener, BroadcastReceiver() {

    private val tag = "NetworkProvider"
    private lateinit var workerThread: WorkerThread
    private lateinit var routeController: RouteController
    private lateinit var eventController: EventController
    private lateinit var locationController: LocationController
    private lateinit var userController: UserController
    private lateinit var carController: CarController
    private lateinit var statusController: StatusController
    private lateinit var firebaseController: FirebaseController

    private val connectivityService = communicationContext.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

    private var isInitialized: Boolean = false

    private var synchronizeTimer: Timer? = null
    private val loginLock: Any = Any()

    private var isSynchronize: AtomicBoolean = AtomicBoolean(false)

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

    private fun onNetworkStatusChanged() {
        synchronized(this) {
            if (canUseConnection()) {
                Log.d(tag, "Network connectivity was changed and it is possible to send data.")

                if (synchronizeTimer == null) {
                    synchronizeTimer = Timer("NetworkSynchronize")
                    synchronizeTimer!!.schedule(getSynchronizeTask(), 1000L, communicationContext.synchronizationInterval)
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

    private fun getSynchronizeTask(): TimerTask {
        return object : TimerTask() {
            override fun run() {

                if (!isSynchronize.getAndSet(true)){
                    Log.d(tag, "Stopping network synchronization thread. Another thread already running.")
                    return
                }

                if (!canUseConnection()) {
                    Log.d(tag, "Stopping network synchronization. Can not use connection.")
                    isSynchronize.set(false)
                    return
                }


                if (!communicationContext.isLogin){
                    Log.d(tag, "Stopping network synchronization thread. User is not login.")
                    return
                }

                sendFirebaseToken()

                val storage = Storage.getInstance(communicationContext.appContext)

                val messages = storage.messageService.getMessages(NetworkProvider.hashCode())
                for (message in messages) {
                    sendEvent(message)
                }

                val routes = storage.routeService.getRoutes()
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
                    val locations = storage.locationService.getLocationsByLocalRouteId(route.uid)
                    for (location in locations) {
                        sendLocation(location)
                    }

                    // Can not remove last route because it is possibility that is still used
                    if (route.uid < maxRouteId && storage.locationService.getLocationsByLocalRouteId(route.uid).isEmpty()) {
                        storage.routeService.deleteRoute(route)
                    }
                }

                val locations = storage.locationService.getLocationsByLocalRouteId(null)
                for (location in locations) {
                    sendLocation(location)
                }

                isSynchronize.set(false)
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {

        when (key) {
            communicationContext.appContext.getString(R.string.key_communication_network_url) -> {
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

            communicationContext.appContext.getString(R.string.key_communication_network_firebase_token) -> {
                Log.d(tag, "Change of Firebase token was detected.")
                sendFirebaseToken()
            }
        }
    }

    override fun initialize(): Boolean {

        if(isInitialized) {
            Log.d(tag,"Already initialized. Nothing to init.")
            return true
        }

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
            val httpClient = OkHttpClient.Builder()
                    .authenticator(TokenAuthenticator(communicationContext.authorizationServerUrl, communicationContext.appContext))
                    .addInterceptor(TokenInterceptor(communicationContext.appContext))
                    .build()
            routeController = RouteController(communicationContext.serverUrl, httpClient)
            eventController = EventController(communicationContext.serverUrl, httpClient)
            locationController = LocationController(communicationContext.serverUrl, httpClient)
            carController = CarController(communicationContext.serverUrl, httpClient)
            statusController = StatusController(communicationContext.serverUrl, httpClient)
            firebaseController = FirebaseController(communicationContext.serverUrl, httpClient)
            userController = UserController(communicationContext.authorizationServerUrl)
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

        if (synchronizeTimer != null) {
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

                val user = Storage.getInstance(communicationContext.appContext).userService.getUser()
                val carId = user?.carId ?: -1L
                if (carId == -1L){
                    Log.d(tag, "Util switch Network message will not be send. Car is not set.")
                    return@Runnable
                }
                val event = EventCreate(messageType, actualTime, carId, note)
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

            val user = Storage.getInstance(communicationContext.appContext).userService.getUser()
            val carId = user?.carId ?: -1L
            if (carId == -1L){
                Log.d(tag, "Event message will not be send. Car is not set.")
                return@Runnable
            }

            val event = EventCreate(getEventType(messageType).toLong(), actualTime, carId, note)
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
                Storage.getInstance(communicationContext.appContext).locationService.saveLocation(location)
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

            val route = Storage.getInstance(communicationContext.appContext).routeService.getRoute(localRouteId)
            sendRoute(route)
        }

        workerThread.postTask(task)
        return true
    }

    override fun sendStatus(battery: Float, isCharging: Boolean, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>): Boolean {
        val longTime = Date().time

        val task = Runnable {
            if (!canSendMessage() || !canUseConnection()) {
                Log.d(tag, "Can not send status. Message will be destroyed.")
                return@Runnable
            }

            val user = Storage.getInstance(communicationContext.appContext).userService.getUser()

            if(user == null){
                Log.w(tag, "Can not send status. User is null")
                return@Runnable
            }

            val status = StatusCreate(battery, isCharging, powerSaveMode, utils, longTime, user.carId)
            val result = statusController.createStatus(status)
            if(result.isSuccessful){
                Log.d(tag, "Status message was send successfully")
            } else {
                Log.d(tag, "Send of status message ends with status code: ${result.code()}")
                logoutIfUnauthorized(result.code())
            }
        }

        workerThread.postTask(task)
        return true
    }

    fun login(username: String, password: String) {

        val longTime = Date().time

        val task = Runnable {

            synchronized(loginLock) {
                if (!canSendWithoutLogin() || !canUseConnection()) {
                    sendLoginBroadcast(false, R.string.err_login_init_network)
                    return@Runnable
                }
                val userService = Storage.getInstance(communicationContext.appContext).userService
                if (userService.getUser() != null) {
                    Log.d(tag, "already logged in")
                    sendLoginBroadcast(false, R.string.err_login_already_login)
                    return@Runnable
                }

                try {
                    val tokenResponse = userController.login(username, password)

                    Log.d(tag, "Login response with code: ${tokenResponse.code()}")
                    when (tokenResponse.code()){
                        200 -> {
                            Log.d(tag, "Parsing login response: ${tokenResponse.body().toString()}")

                            val token = Token(tokenResponse.body() as LinkedTreeMap<*, *>)
                            userService.saveUser(User(token, username, longTime))

                            Log.d(tag, "User successfully logged in")
                            sendLoginBroadcast(true, 0)
                        }
                        400, 401, 403 -> sendLoginBroadcast(false, R.string.err_login_credentials)
                        else -> sendLoginBroadcast(false, R.string.err_login_bad_request, tokenResponse.code())
                    }

                } catch (e: Exception) {
                    Log.e(tag, "Can not login user. Cause: ${e.message}")
                    sendLoginBroadcast(false, R.string.err_login_failed)
                }
            }
        }

        workerThread.postTask(task)
    }

    fun loginSuccess(){
        // Send Firebase token to server
        FirebaseService().updateFirebaseToken(communicationContext.appContext)
        sendFirebaseToken()
    }

    private fun sendLoginBroadcast(success: Boolean, errorMessageResId: Int, vararg args: Any) {
        val intent = Intent(LoginFragment.BroadcastKeys.BroadcastLoginResult.name)

        intent.putExtra(LoginFragment.BroadcastKeys.KeySuccess.name, success)
        if (!success) {
            val message = communicationContext.appContext.getString(errorMessageResId, *args)
            intent.putExtra(LoginFragment.BroadcastKeys.KeyErrorMessage.name, message)
        }
        LocalBroadcastManager.getInstance(communicationContext.appContext).sendBroadcast(intent)
    }

    fun getCars() {
        val task = Runnable {
            if (!canSendMessage() || !canUseConnection()) {
                sendLoginBroadcast(false, R.string.err_login_init_network)
                return@Runnable
            }

            try {
                val carsResponse = carController.getCars()
                if (carsResponse.isSuccessful) {

                    Log.d(tag, "Parsing get cars response: ${carsResponse.body().toString()}")

                    val list = ArrayList<Serializable>()

                    for (car in carsResponse.body() as Collection<*>) {
                        if (car is LinkedTreeMap<*, *>) {
                            list.add(car)
                        }
                    }
                    sendGetCarsBroadcast(list, -1)

                } else {
                    Log.d(tag, "Can not get cars: $carsResponse")
                    logoutIfUnauthorized(carsResponse.code())
                    sendGetCarsBroadcast(arrayListOf(), R.string.err_login_bad_request, carsResponse.code())
                }

            } catch (e: Exception) {
                Log.e(tag, "Can not get cars. Cause: ${e.message}")
                sendGetCarsBroadcast(arrayListOf(), R.string.err_get_cars_failed)
            }
        }

        workerThread.postTask(task)
    }

    private fun sendGetCarsBroadcast(cars: ArrayList<Serializable>, errorMessageResId: Int, vararg args: Any) {
        val intent = Intent(LoginFragment.BroadcastKeys.BroadcastGetCarsResult.name)

        if (errorMessageResId > 0) {
            val message = communicationContext.appContext.getString(errorMessageResId, *args)
            intent.putExtra(LoginFragment.BroadcastKeys.KeyErrorMessage.name, message)
        } else {
            intent.putExtra(LoginFragment.BroadcastKeys.KeyCars.name, cars)
        }
        LocalBroadcastManager.getInstance(communicationContext.appContext).sendBroadcast(intent)
    }

    fun createCar(name: String) {

        val task = Runnable {
            if (!canSendMessage() || !canUseConnection()) {
                sendLoginBroadcast(false, R.string.err_login_init_network)
                return@Runnable
            }

            try {
                val carResponse = carController.createCar(name)

                when (carResponse.code()) {

                    200, 201 -> {
                        val carId = (carResponse.body() as LinkedTreeMap<*, *>)["car_id"] as String

                        val userService = Storage.getInstance(communicationContext.appContext).userService

                        val user = userService.getUser()
                        if (user != null) {
                            user.carName = name
                            user.carId = carId.toLong()
                            userService.updateUser(user)
                            Log.d(tag, "User updated successfully")
                        }

                        sendCreateCarBroadcast(-1) // success
                    }
                    else -> {
                        Log.d(tag, "Can not create car: $carResponse")
                        logoutIfUnauthorized(carResponse.code())
                        sendCreateCarBroadcast(R.string.err_create_car_failed)
                    }
                }

            } catch (e: Exception) {
                Log.e(tag, "Can not create car. Cause: ${e.message}")
                sendCreateCarBroadcast(R.string.err_create_car_failed)
            }
        }
        workerThread.postTask(task)
    }

    private fun sendCreateCarBroadcast(errorMessageResId: Int, vararg args: Any) {
        val intent = Intent(LoginFragment.BroadcastKeys.BroadcastCreateCarsResult.name)

        if (errorMessageResId > 0) {
            val message = communicationContext.appContext.getString(errorMessageResId, *args)
            intent.putExtra(LoginFragment.BroadcastKeys.KeyErrorMessage.name, message)
        }

        LocalBroadcastManager.getInstance(communicationContext.appContext).sendBroadcast(intent)
    }


    /**
     * Can not run in main thread
     */
    private fun sendEvent(msg: Message) {

        var inDB = true

        if (canUseConnection()) {
            val result = eventController.createEvent(msg.message)
            Log.d(tag, "SendEvent response: ${result.code()}")

            if (result.isSuccessful) {
                inDB = false
            } else {
                if(logoutIfUnauthorized(result.code()))return // Logout and all data cleared nothing to do
            }
        }

        if (inDB && msg.uid == 0) {
            // should be in DB but msg.uid == 0 => it is not in DB
            Storage.getInstance(communicationContext.appContext).messageService.saveMessage(msg)
            Log.d(tag, "Event was stored to database.")
        } else if (!inDB && msg.uid != 0) {
            // should not be in DB but msg.uid != 0 => it is in DB
            Storage.getInstance(communicationContext.appContext).messageService.deleteMessage(msg)
        }
    }

    /**
     * Can not run in main thread
     */
    private fun sendLocation(location: Location) {

        var inDB = true
        var update = false
        var send = true
        val storage = Storage.getInstance(communicationContext.appContext)

        if (canUseConnection()) {

            if (location.localRouteId != null && location.routeId == null) {
                val route = storage.routeService.getRoute(location.localRouteId!!)
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
                Log.d(tag, "SendLocation response: ${result.code()}")
                if (result.isSuccessful) {
                    inDB = false
                } else{
                    if (logoutIfUnauthorized(result.code())) return // Logout and all data cleared nothing to do
                    if (result.code() == HttpURLConnection.HTTP_CONFLICT) inDB = false // already in DB
                }
            }
        }

        if (inDB && location.uid != 0 && update) {
            // should be in DB it is in DB but need to be updated
            storage.locationService.updateLocation(location)
        } else if (inDB && location.uid == 0) {
            // should be in DB but location.uid == 0 => it is not in DB
            storage.locationService.saveLocation(location)
        } else if (!inDB && location.uid != 0) {
            // should not be in DB but location.uid != 0 => it is in DB
            storage.locationService.deleteLocations(listOf(location))
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
            val storage = Storage.getInstance(communicationContext.appContext)

            val response = routeController.createRoute(route.carId, route.time)
            Log.d(tag, "SendRoute response: ${response.code()}")

            if (response.isSuccessful) {
                val jsonResponse = JsonParser().parse(response.body().toString()).asJsonObject
                val serverRouteId = jsonResponse["route_id"].asLong

                route.serverRouteId = serverRouteId
                if (route.uid == 0) {
                    storage.routeService.saveRoute(route)
                } else {
                    storage.routeService.updateRoute(route)
                }

                Log.d(tag, "Route was successfully created and stored to DB")
            } else {
                if (logoutIfUnauthorized(response.code())) return // Logout and all data cleared nothing to do
            }
        }

        if (route.uid == 0) {
            Storage.getInstance(communicationContext.appContext).routeService.saveRoute(route)
        }
    }

    private fun sendFirebaseToken(){

        if(communicationContext.firebaseToken.isBlank()){
            Log.d(tag, "No Firebase token to save")
            return
        }

        val task = Runnable {
            if (!canSendMessage() || !canUseConnection()) {
                return@Runnable
            }

            val user = Storage.getInstance(communicationContext.appContext).userService.getUser()
            if(user == null) {
                Log.d(tag, "Can not send token. User is not logged in.")
                return@Runnable
            }

            if (user.carId == -1L){
                Log.d(tag, "Can not send token. User did not select car yet.")
                return@Runnable
            }

            val response = firebaseController.saveToken(user.carId, communicationContext.firebaseToken)
            if(response.isSuccessful) {
                Log.d(tag, "Firebase token send successfully to server")
                communicationContext.firebaseToken = ""
            } else {
                Log.w(tag, "Send Firebase token failed. Status code: ${response.code()}")
            }
        }
        workerThread.postTask(task)
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

    private fun canSendWithoutLogin(): Boolean {
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

    private fun canSendMessage(): Boolean {

        if (!communicationContext.isLogin) {
            Log.d(tag, "Can not send message. User is not logged in.")
            return false
        }

        return canSendWithoutLogin()
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

    private fun logoutIfUnauthorized(statusCode: Int): Boolean{
        if (statusCode == 401 || statusCode == 403){
            Log.d(tag, "Response status code: $statusCode. Logging out user from application.")
            communicationContext.isLogin = false
            return true
        }
        return false
    }
}