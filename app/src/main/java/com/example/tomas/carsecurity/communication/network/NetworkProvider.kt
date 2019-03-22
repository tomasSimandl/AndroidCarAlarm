package com.example.tomas.carsecurity.communication.network

import android.Manifest
import android.annotation.SuppressLint
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
import com.example.tomas.carsecurity.communication.network.config.TokenAuthenticator
import com.example.tomas.carsecurity.communication.network.config.TokenInterceptor
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
import com.example.tomas.carsecurity.tools.ToolsEnum
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.internal.LinkedTreeMap
import okhttp3.OkHttpClient
import java.io.Serializable
import java.net.HttpURLConnection
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList


/**
 * Class is used for communication over network.
 */
class NetworkProvider(private val communicationContext: CommunicationContext) :
        ICommunicationProvider,
        SharedPreferences.OnSharedPreferenceChangeListener,
        BroadcastReceiver() {

    /** Logger tag */
    private val tag = "NetworkProvider"

    /** Thread on which is scheduled all network communication. */
    private lateinit var workerThread: WorkerThread

    /** Controller for communication with server over Route endpoint. */
    private lateinit var routeController: RouteController
    /** Controller for communication with server over Event endpoint. */
    private lateinit var eventController: EventController
    /** Controller for communication with server over Location endpoint. */
    private lateinit var locationController: LocationController
    /** Controller for communication with server over User endpoint. */
    private lateinit var userController: UserController
    /** Controller for communication with server over Car endpoint. */
    private lateinit var carController: CarController
    /** Controller for communication with server over Status endpoint. */
    private lateinit var statusController: StatusController
    /** Controller for communication with server over Firebase endpoint. */
    private lateinit var firebaseController: FirebaseController

    /**
     * Connectivity service user for getting connection status (Connected/Disconnected) and type
     * of connection (Cellular/Wifi)
     */
    private val connectivityService = communicationContext.appContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

    /** Indicates if this class was already successfully initialized */
    private var isInitialized: Boolean = false

    /** Timer for scheduling network synchronization tasks. */
    private var synchronizeTimer: Timer? = null

    /** Lock to avoid sending of two login request at the same time */
    private val loginLock: Any = Any()

    /** Maximal number of locations which are sends over one request. */
    private val locationChunkSize: Int

    /**
     * Indication if network synchronization task is already running. Can happen when there is lots
     * of data to synchronize and synchronize interval is short.
     */
    private var isSynchronize: AtomicBoolean = AtomicBoolean(false)


    /**
     * Object is used for check if NetworkProvider can be initialized.
     */
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

    /**
     * Loads maximal size of location request [locationChunkSize] from properties. Default value is 30 and
     * range of number is <1, 1000>
     */
    init {
        val chunksProperty = communicationContext.properties["send.locations.max.chunk.size"] as String?
        val chunks: Int = chunksProperty?.toIntOrNull() ?: 30
        locationChunkSize = chunks.coerceIn(1, 1000)
    }

    /**
     * Broadcast receiver method react only on Connectivity change. Deprecated method can be used because broadcast is
     * not registered in manifest but programmatically in initialize method.
     */
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            onNetworkStatusChanged()
        }
    }

    /**
     * Method only check connection and initialize or destroy actual synchronizeTimer. Method is synchronized to this
     * object.
     */
    private fun onNetworkStatusChanged() {
        synchronized(this) {
            if (canUseConnection()) {
                Log.d(tag, "Network connectivity was changed and it is possible to send data.")
                initSynchronizeTimer()
            } else {
                Log.d(tag, "Network connectivity was changed. Can not send data.")
                destroySynchronizeTimer()
            }
        }
    }

    /**
     * Variable which on every get request create new object of TimerTask which contains network synchronization logic.
     * When communication is allowed first is send FirebaseToken, than events, routes, locations of routes and locations
     * without routes.
     */
    private val synchronizeTask: TimerTask
        get() = object : TimerTask() {
            override fun run() {

                if (!isSynchronize.getAndSet(true)) {
                    Log.d(tag, "Stopping network synchronization thread. Another thread already running.")
                    return
                }

                if (!canUseConnection()) {
                    Log.d(tag, "Stopping network synchronization. Can not use connection.")
                    isSynchronize.set(false)
                    return
                }


                if (!communicationContext.isLogin) {
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
                    // send routes locations in package by 50 locations
                    locations.chunked(locationChunkSize).forEach { sendLocations(it) }

                    // Can not remove last route because it is possibility that is still used
                    if (route.uid < maxRouteId && storage.locationService.getLocationsByLocalRouteId(route.uid).isEmpty()) {
                        storage.routeService.deleteRoute(route)
                    }
                }

                // send locations in package by 50 locations
                val locations = storage.locationService.getLocationsByLocalRouteId(null)
                locations.chunked(locationChunkSize).forEach { sendLocations(it) }

                isSynchronize.set(false)
            }
        }

    /**
     * This method is automatically called when same value in SharedPreferences is changed. Method responds on change
     * of this preferences:
     *
     * communication_network_url
     * communication_network_cellular
     * communication_network_update_interval
     * communication_network_firebase_token
     * communication_network_is_user_login
     */
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
                    destroySynchronizeTimer()
                }
                onNetworkStatusChanged()
            }

            communicationContext.appContext.getString(R.string.key_communication_network_firebase_token) -> {
                Log.d(tag, "Change of Firebase token was detected.")
                sendFirebaseToken()
            }

            communicationContext.appContext.getString(R.string.key_communication_network_is_user_login) -> {
                if (!communicationContext.isLogin) {
                    Thread(Runnable {
                        Log.d(tag, "User logout. Clearing database.")
                        Storage.getInstance(communicationContext.appContext).clearAllTables()
                    }).start()
                }
            }
        }
    }

    /**
     * Method initialize NetworkProvider. When initialization is not possible false is returned.
     *
     * @return true on success initialization, false otherwise.
     */
    override fun initialize(): Boolean {

        if (isInitialized) {
            Log.d(tag, "Already initialized. Nothing to init.")
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

            onNetworkStatusChanged()
            isInitialized = true
            return true
        }
        return false
    }

    /**
     * Method initialize all controllers which are used for network communication.
     *
     * @return if initialization was successful
     */
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
            userController = UserController(communicationContext.authorizationServerUrl, communicationContext.appContext)
            true
        } catch (e: Exception) {
            Log.e(tag, "Can not initialize Controllers: $e")
            false
        }
    }

    /**
     * Deinitialize whole NetworkProvider. This method should be called before instance destruction.
     */
    override fun destroy() {
        Log.d(tag, "Destroying")
        isInitialized = false
        if (::workerThread.isInitialized) workerThread.quit()
        communicationContext.unregisterOnPreferenceChanged(this)
        communicationContext.appContext.unregisterReceiver(this)

        destroySynchronizeTimer()
    }

    /**
     * Return if NetworkProvider is successfully initialized.
     *
     * @return if NetworkProvider is successfully initialized.
     */
    override fun isInitialize(): Boolean {
        return isInitialized
    }

    /**
     * Send information about activation or deactivation of util. Message is as Event.
     *
     * Request is send in workerThread thread.
     *
     * @param toolsEnum enum which identifies util which was changed
     * @param enabled indicates if util was activate - true or deactivate - false
     */
    override fun sendUtilSwitch(toolsEnum: ToolsEnum, enabled: Boolean): Boolean {
        val actualTime = Calendar.getInstance().timeInMillis

        val task = Runnable {
            if (!canSendMessage()) return@Runnable

            if (communicationContext.isMessageAllowed(this.javaClass.name, "Tool_State_Changed_send")) {
                Log.d(tag, "Sending util switch network message of util: ${toolsEnum.name}.")

                val messageType: Long
                val note: String
                if (enabled) {
                    messageType = communicationContext.appContext.resources.getInteger(R.integer.event_util_switch_on).toLong()
                    note = "Util ${toolsEnum.name} was turned on."
                } else {
                    messageType = communicationContext.appContext.resources.getInteger(R.integer.event_util_switch_off).toLong()
                    note = "Util ${toolsEnum.name} was turned off."
                }

                val user = Storage.getInstance(communicationContext.appContext).userService.getUser()
                val carId = user?.carId ?: -1L
                if (carId == -1L) {
                    Log.d(tag, "Util switch Network message will not be send. Car is not set.")
                    return@Runnable
                }
                val event = EventCreate(messageType, actualTime, carId, note)
                val eventStr = Gson().toJson(event)
                val message = Message(communicatorHash = NetworkProvider.hashCode(), message = eventStr)

                sendEvent(message)
            } else {
                Log.d(tag, "Util switch Network message is not allowed for util ${toolsEnum.name}.")
            }
        }
        workerThread.postTask(task)
        return true
    }

    /**
     * Send event given by message type and message arguments to server.
     *
     * Request is send in workerThread thread.
     *
     * @param messageType is type of event message which should be send to server.
     * @param args are arguments to message which is used as a note.
     */
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
            if (carId == -1L) {
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

    /**
     * Send input location to server or store it to database.
     *
     * Request is send in workerThread thread.
     *
     * @param location which will be send to server
     * @param isAlarm indicates if send position request is produced by alarm
     * @param cache indicates if location shoud be stored in database for later sending or send immediately
     * @return true
     */
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
                sendLocations(listOf(location))
            }
        }
        workerThread.postTask(task)
        return true
    }

    /**
     * Send route to server. Route is taken from Room database.
     *
     * Request is send in workerThread thread.
     *
     * @param localRouteId id of route in local Room database which will be send to server
     * @return true
     */
    override fun sendRoute(localRouteId: Int): Boolean {
        val task = Runnable {
            if (!canSendMessage()) return@Runnable

            val route = Storage.getInstance(communicationContext.appContext).routeService.getRoute(localRouteId)
            sendRoute(route)
        }

        workerThread.postTask(task)
        return true
    }

    /**
     * Method send status to server. Status is created form all input parameters. When user is not login or send of
     * message was unsuccessful, status message is deleted.
     *
     * Request is send in workerThread thread.
     *
     * @param battery capacity level, 0 - empty, 1 - fully charged.
     * @param isCharging indicates if device is connected to external power source
     * @param powerSaveMode indicates if device is in power save mode
     * @param tools list of activated tools
     * @return true
     */
    override fun sendStatus(battery: Float, isCharging: Boolean, powerSaveMode: Boolean, tools: Map<ToolsEnum, Boolean>): Boolean {
        val longTime = Date().time

        val task = Runnable {
            if (!canSendMessage() || !canUseConnection()) {
                Log.d(tag, "Can not send status. Message will be destroyed.")
                return@Runnable
            }

            val user = Storage.getInstance(communicationContext.appContext).userService.getUser()

            if (user == null) {
                Log.w(tag, "Can not send status. User is null")
                return@Runnable
            }

            val status = StatusCreate(battery, isCharging, powerSaveMode, tools, longTime, user.carId)
            val result = statusController.createStatus(status)
            if (result.isSuccessful) {
                Log.d(tag, "Status message was send successfully")
            } else {
                Log.d(tag, "Send of status message ends with status code: ${result.code()}")
                logoutIfUnauthorized(result.code())
            }
        }

        workerThread.postTask(task)
        return true
    }

    /**
     * Method creates login request to authorization server. Response is produced over sendLoginBroadcast method.
     *
     * Request is send in workerThread thread.
     *
     * @param username of user
     * @param password of user
     */
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
                    when (tokenResponse.code()) {
                        200 -> {
                            Log.d(tag, "Parsing login response: ${tokenResponse.body().toString()}")

                            val token = Token(tokenResponse.body() as LinkedTreeMap<*, *>)
                            userService.saveUser(User(token, username, longTime))

                            Log.d(tag, "User successfully logged in")
                            sendLoginBroadcast(true)
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

    /**
     * This call should be called when login was successfully handle by application logic. Method load and send Firebase
     * token to server.
     */
    fun loginSuccess() {
        FirebaseService().updateFirebaseToken(communicationContext.appContext)
        sendFirebaseToken()
    }

    /**
     * Method send create get cars request to server over CarController. Response is produce over sendGetCarsBroadcast
     * method.
     *
     * Request is send in workerThread thread.
     */
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
                    sendGetCarsBroadcast(list)

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

    /**
     * Method send create car request to server over CarController. Response is produce over sendCreateCarBroadcast
     * method.
     *
     * Request is send in workerThread thread.
     *
     * @param name name of car which should be created
     */
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

                        sendCreateCarBroadcast() // success
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

    /**
     * Method send Broadcast message to LoginFragment with result of login request.
     * If input success is false error message given by errorMessageResId is append to broadcast.
     *
     * @param success indication if login was successfully or not
     * @param errorMessageResId id of string resource which contains required error message
     * @param args arguments to input error message given by resource id
     */
    private fun sendLoginBroadcast(success: Boolean, errorMessageResId: Int = -1, vararg args: Any) {
        val intent = Intent(LoginFragment.BroadcastKeys.BroadcastLoginResult.name)

        intent.putExtra(LoginFragment.BroadcastKeys.KeySuccess.name, success)
        if (!success) {
            val message = communicationContext.appContext.getString(errorMessageResId, *args)
            intent.putExtra(LoginFragment.BroadcastKeys.KeyErrorMessage.name, message)
        }
        LocalBroadcastManager.getInstance(communicationContext.appContext).sendBroadcast(intent)
    }

    /**
     * Method send Broadcast message to LoginFragment with list of users cars.
     * If input errorMessageResourceId is higher than zero, error message is append to broadcast.
     *
     * @param cars list of users cars which will be send wia broadcast
     * @param errorMessageResId id of string resource which contains required error message
     * @param args arguments to input error message given by resource id
     */
    private fun sendGetCarsBroadcast(cars: ArrayList<Serializable>, errorMessageResId: Int = -1, vararg args: Any) {
        val intent = Intent(LoginFragment.BroadcastKeys.BroadcastGetCarsResult.name)

        if (errorMessageResId > 0) {
            val message = communicationContext.appContext.getString(errorMessageResId, *args)
            intent.putExtra(LoginFragment.BroadcastKeys.KeyErrorMessage.name, message)
        } else {
            intent.putExtra(LoginFragment.BroadcastKeys.KeyCars.name, cars)
        }
        LocalBroadcastManager.getInstance(communicationContext.appContext).sendBroadcast(intent)
    }

    /**
     * Method send Broadcast message to LoginFragment with information about create car request.
     * If input errorMessageResourceId is higher than zero, error message is append to broadcast.
     *
     * @param errorMessageResId id of string resource which contains error message
     * @param args arguments to input string resource id
     */
    private fun sendCreateCarBroadcast(errorMessageResId: Int = -1, vararg args: Any) {
        val intent = Intent(LoginFragment.BroadcastKeys.BroadcastCreateCarsResult.name)

        if (errorMessageResId > 0) {
            val message = communicationContext.appContext.getString(errorMessageResId, *args)
            intent.putExtra(LoginFragment.BroadcastKeys.KeyErrorMessage.name, message)
        }

        LocalBroadcastManager.getInstance(communicationContext.appContext).sendBroadcast(intent)
    }

    /**
     * Method send input message to server. If sending is not successful message is stored in database for later sending.
     * On success sending, message is removed from database if it is in database.
     *
     * Request is send on callers thread.
     *
     * @param msg message with event which should be sent to server
     */
    private fun sendEvent(msg: Message) {

        var inDB = true

        if (canUseConnection()) {
            val result = eventController.createEvent(msg.message)
            Log.d(tag, "SendEvent response: ${result.code()}")

            if (result.isSuccessful) {
                inDB = false
            } else {
                if (logoutIfUnauthorized(result.code())) return // Logout and all data cleared nothing to do
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
     * Method store input locations to database, set remote server route id according to specified route id and return
     * list of only locations which have set remote route id.
     *
     * @param locations list of locations that should be prepared for sending.
     * @return list of locations which can be send to server.
     */
    @SuppressLint("UseSparseArrays")
    private fun prepareLocation(locations: List<Location>): List<Location> {

        val storage = Storage.getInstance(communicationContext.appContext)
        // routes ids cache <localRouteId, remoteRouteId>
        val routes = HashMap<Int, Long?>()
        val locationsToSend = ArrayList<Location>()

        for (location in locations) {

            if (location.uid == 0) {
                // if location is not in db store it in db
                storage.locationService.saveLocation(location)
            }

            if (location.localRouteId != null && location.routeId == null) {
                // try set remote route id to location
                val routeId: Long? = if (routes.containsKey(location.localRouteId!!)) {
                    routes[location.localRouteId!!]
                } else {
                    val route = storage.routeService.getRoute(location.localRouteId!!)
                    routes[location.localRouteId!!] = route.serverRouteId
                    route.serverRouteId
                }

                if (routeId == null) {
                    Log.w(tag, "Can not send position because route was not created yet.")
                } else {
                    location.routeId = routeId
                    locationsToSend.add(location)
                    storage.locationService.updateLocation(location)
                }

            } else if (location.routeId != null) {
                locationsToSend.add(location)
            }
        }
        return locationsToSend
    }

    /**
     * Method send and list of location to server.
     * Before locations is send, method get routes associated with positions and set them remote route id. When route
     * was not created on server, positions are not send to server.
     * If location is not successfully send to server it is stored in database.
     * Successfully send positions are removed from database. Successfully means returned status code 201 Created and
     * 409 Conflict because that means that positions are already stored on server.
     *
     * Request is send on callers thread.
     *
     * @param locations list of locations which should be send to server.
     */
    private fun sendLocations(locations: List<Location>) {

        var removePositions = false

        if (canUseConnection()) {

            val locationsToSend = prepareLocation(locations)

            if (locationsToSend.isNotEmpty()) {
                val result = locationController.createLocations(locationsToSend)
                Log.d(tag, "SendLocations response: ${result.code()}")
                if (result.isSuccessful) {
                    removePositions = true
                } else {
                    if (logoutIfUnauthorized(result.code())) return // Logout and all data cleared nothing to do
                    if (result.code() == HttpURLConnection.HTTP_CONFLICT) removePositions = true
                }

                if (removePositions) {
                    val storage = Storage.getInstance(communicationContext.appContext)
                    storage.locationService.deleteLocations(locationsToSend)
                }
            }
        }
    }

    /**
     * Method create input route on server.
     * When route is already created on server method did not do any action.
     * When route is created successfully, route in local Room database is updated with id of route on server.
     * When route is not create on server successfully it is also saved in database for later sending.
     *
     * Request is send in thread of caller.
     *
     * @param route route which should be created on server.
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

    /**
     * Method take Firebase token from SharedPreferences and if it is not empty send it to server over
     * FirebaseController. Request is send with workerThread thread.
     */
    private fun sendFirebaseToken() {

        if (communicationContext.firebaseToken.isBlank()) {
            Log.d(tag, "No Firebase token to save")
            return
        }

        val task = Runnable {
            if (!canSendMessage() || !canUseConnection()) {
                return@Runnable
            }

            val user = Storage.getInstance(communicationContext.appContext).userService.getUser()
            if (user == null) {
                Log.d(tag, "Can not send token. User is not logged in.")
                return@Runnable
            }

            if (user.carId == -1L) {
                Log.d(tag, "Can not send token. User did not select car yet.")
                return@Runnable
            }

            val response = firebaseController.saveToken(user.carId, communicationContext.firebaseToken)
            if (response.isSuccessful) {
                Log.d(tag, "Firebase token send successfully to server")
                communicationContext.firebaseToken = ""
            } else {
                Log.w(tag, "Send Firebase token failed. Status code: ${response.code()}")
            }
        }
        workerThread.postTask(task)
    }

    /**
     * Method only returns if device is connected to network.
     *
     * @return if device is connected to network.
     */
    private fun isConnected(): Boolean {
        return connectivityService?.activeNetworkInfo?.isConnected ?: false
    }

    /**
     * Method return if actual network connection is cellular or any other.
     *
     * @return true if network connection is cellular, false - otherwise
     */
    private fun isCellular(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            connectivityService?.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
        } else {
            val activeNetwork = connectivityService?.activeNetwork ?: return false

            val capabilities = connectivityService.getNetworkCapabilities(activeNetwork)
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }
    }

    /**
     * Method return if some message can be send to server. This method do not check network connection and do not check
     * if user is login.
     *
     * @return if message can be send.
     */
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

    /**
     * Method returns if some message which required logged user can be send to server. This method do not check
     * network connection.
     *
     * @return if message can be send.
     */
    private fun canSendMessage(): Boolean {

        if (!communicationContext.isLogin) {
            Log.d(tag, "Can not send message. User is not logged in.")
            return false
        }

        return canSendWithoutLogin()
    }

    /**
     * Method returns if communication layer can be used for sending any message.
     *
     * @return true if connection can be used, false - otherwise
     */
    private fun canUseConnection(): Boolean {
        return isConnected() && (communicationContext.cellular || !isCellular())
    }

    /**
     * Method returns event type id based on input message type. Event type is defined by server event types.
     *
     * @param messageType message type of which we request event type id
     * @return event type id
     */
    private fun getEventType(messageType: MessageType): Int {
        return when (messageType) {
            MessageType.UtilSwitch,
            MessageType.AlarmLocation,
            MessageType.Location,
            MessageType.Status ->
                communicationContext.appContext.resources.getInteger(R.integer.event_unknown)
            MessageType.Alarm ->
                communicationContext.appContext.resources.getInteger(R.integer.event_alarm_on)
            MessageType.BatteryWarn ->
                communicationContext.appContext.resources.getInteger(R.integer.event_battery)
            MessageType.PowerConnected ->
                communicationContext.appContext.resources.getInteger(R.integer.event_power_connected)
            MessageType.PowerDisconnected ->
                communicationContext.appContext.resources.getInteger(R.integer.event_power_disconnected)
        }
    }

    /**
     * Method check if event message with given type is allowed to send.
     *
     * @param messageType type of event message which will be checked
     * @return true if message is allowed, false - otherwise
     */
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

    /**
     * This method should be called on most of responses. When server return unauthorized actual login user will be
     * logout. Application should never request unauthorized request this means that unauthorized is return only when
     * user is logout from server.
     *
     * Method change login status in shared preferences based on input status code.
     *
     * @param statusCode returned http status code
     * @return true when user will be logout, false - otherwise
     */
    private fun logoutIfUnauthorized(statusCode: Int): Boolean {
        if (statusCode == 401 || statusCode == 403) {
            Log.d(tag, "Response status code: $statusCode. Logging out user from application.")
            communicationContext.isLogin = false
            return true
        }
        return false
    }

    /**
     * Method initialize network synchronization timer but only if it is not initialized.
     */
    private fun initSynchronizeTimer() {
        if (synchronizeTimer == null) {
            synchronizeTimer = Timer("NetworkSynchronize")
            synchronizeTimer!!.schedule(synchronizeTask, 1000L, communicationContext.synchronizationInterval)
        }
    }

    /**
     * Method deinitialize network synchronization timer but only if it is initialized.
     */
    private fun destroySynchronizeTimer() {
        if (synchronizeTimer != null) {
            synchronizeTimer!!.cancel()
            synchronizeTimer = null
        }
    }
}