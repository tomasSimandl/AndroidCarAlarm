package com.example.tomas.carsecurity.communication

import android.content.SharedPreferences
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.network.NetworkProvider
import com.example.tomas.carsecurity.communication.sms.SmsProvider
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.tools.UtilsEnum

/**
 * Class manage all communication providers which are supported by application.
 * This class is Singleton.
 */
class CommunicationManager private constructor(private val communicationContext: CommunicationContext)
    : SharedPreferences.OnSharedPreferenceChangeListener {

    /** Logger tag */
    private val tag = "CommunicationManager"
    /** List of all activated communication providers */
    private val activeCommunicators: MutableSet<ICommunicationProvider> = HashSet()

    /** Object for realisation of Singleton */
    companion object {
        /** Instance of CommunicationManager */
        private var instance: CommunicationManager? = null

        /**
         * At first call create new instance of CommunicationManager. On every another call returns the same instance.
         *
         * @param communicationContext it is used only at first call of this method for create an instance.
         * @return instance of CommunicationManager
         */
        fun getInstance(communicationContext: CommunicationContext): CommunicationManager {
            if (instance == null) {
                Log.d("CommManager.instance", "Creating new instance of Communication manager.")
                instance = CommunicationManager(communicationContext)
            }
            return instance!!
        }
    }

    /**
     * Constructor initialize all possible communication providers and register this class to listen of change of
     * SharedPreferences.
     */
    init {
        tryInitializeProvider(SmsProvider(communicationContext))
        tryInitializeProvider(NetworkProvider(communicationContext))

        communicationContext.registerOnPreferenceChanged(this)
    }

    /**
     * Method call initialize method of given provider. If initialization is successful, provider is added to
     * activeCommunicators.
     *
     * @param provider communication provider which should not be already successfully initialized.
     */
    private fun tryInitializeProvider(provider: ICommunicationProvider) {
        if (provider.initialize()) {
            activeCommunicators.add(provider)
        }
    }

    /**
     * Method is automatically called when any value in sharedPreferences is changed. Method make some action only on
     * preferences:
     * communication_sms_is_allowed
     * communication_network_is_allowed
     * communication_sms_phone_number
     * communication_network_url
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            communicationContext.appContext.getString(R.string.key_communication_sms_is_allowed) -> {
                if (canRegisterProvider(activeCommunicators.find { it is SmsProvider }, sharedPreferences, key))
                    tryInitializeProvider(SmsProvider(communicationContext))
            }

            communicationContext.appContext.getString(R.string.key_communication_network_is_allowed) -> {
                if (canRegisterProvider(activeCommunicators.find { it is NetworkProvider }, sharedPreferences, key))
                    tryInitializeProvider(NetworkProvider(communicationContext))
            }

            communicationContext.appContext.getString(R.string.key_communication_sms_phone_number) -> {
                val provider = activeCommunicators.find { it is SmsProvider }
                if (provider != null && SmsProvider.check(communicationContext.appContext) != CheckCodes.success) {
                    provider.destroy()
                    activeCommunicators.remove(provider)
                }
            }

            communicationContext.appContext.getString(R.string.key_communication_network_url) -> {
                val provider = activeCommunicators.find { it is NetworkProvider }
                if (provider != null && NetworkProvider.check(communicationContext.appContext) != CheckCodes.success) {
                    provider.destroy()
                    activeCommunicators.remove(provider)
                }
            }
        }
    }

    /**
     * Method resolve if provider should be created. When provider is not allowed but already
     * exists, it is removed from active providers.
     *
     * @param provider which can be register
     * @param sharedPreferences open access to shared preferences
     * @param key key to shared preferences value on which value is decide if provider can be register
     * @return true when provider should be registered, false - otherwise
     */
    private fun canRegisterProvider(provider: ICommunicationProvider?, sharedPreferences: SharedPreferences, key: String): Boolean {

        if (sharedPreferences.getBoolean(key, false)) {
            // new value is true
            if (provider == null) {
                // provider is not registered yet
                return true
            }
        } else {
            //new value is false
            if (provider != null) {
                provider.destroy()
                activeCommunicators.remove(provider)
            }
        }
        return false
    }

    /**
     * Method unregister all registered communication providers.
     * Method should be called before destroy instance of this class.
     */
    fun destroy() {
        Log.d(tag, "Destroying")
        communicationContext.unregisterOnPreferenceChanged(this)
        activeCommunicators.forEach { it.destroy() }
        activeCommunicators.clear()
    }

    /**
     * Method send information message about util status changed to every active communication provider.
     *
     * @param util enum which identify which util was changed
     * @param enabled true - util activation, false util - deactivation
     */
    fun sendUtilSwitch(util: UtilsEnum, enabled: Boolean) {
        for (provider in activeCommunicators) {
            provider.sendUtilSwitch(util, enabled)
        }
    }

    /**
     * Send information message with event to every active communication provider.
     *
     * @param messageType identification of which type of event will be send
     * @param args arguments of event
     */
    fun sendEvent(messageType: MessageType, vararg args: String) {
        for (provider in activeCommunicators) {
            provider.sendEvent(messageType, *args)
        }
    }

    /**
     * Send given location to every active communication provider.
     *
     * @param location is location which will be send
     * @param isAlarm indication if send location request is caused by alarm
     * @param cache indication if message should be send immediately or cached.
     */
    fun sendLocation(location: Location, isAlarm: Boolean, cache: Boolean = false) {
        for (provider in activeCommunicators) {
            provider.sendLocation(location, isAlarm, cache)
        }
    }

    /**
     * Send status message only over provider which is specified by [communicatorHash] parameter. When provider is
     * not active. Message is ignored.
     *
     * @param communicatorHash hash of any communication provider which should send this message
     * @param battery battery cappacity level
     * @param isCharging identification if device is connected to external source of power
     * @param powerSaveMode identification if device is in power save mode
     * @param utils list of active communication providers
     */
    fun sendStatus(communicatorHash: Int, battery: Float, isCharging: Boolean, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>) {
        for (provider in activeCommunicators) {
            if (provider::class.java.hashCode() == communicatorHash) {
                provider.sendStatus(battery, isCharging, powerSaveMode, utils)
                break
            }
        }
    }

    /**
     * Method send login request only over NetworkProvider.
     *
     * @param username of login user
     * @param password of login user
     * @return true when provider is active, false otherwise
     */
    fun sendNetworkLogin(username: String, password: String): Boolean {
        for (provider in activeCommunicators) {
            if (provider is NetworkProvider) {
                provider.login(username, password)
                return true
            }
        }
        return false
    }

    /**
     * Method send only to NetworkProvider that login was handled successfully.
     * @return true when provider is active, false otherwise.
     */
    fun networkLoginSuccess(): Boolean {
        for (provider in activeCommunicators) {
            if (provider is NetworkProvider) {
                provider.loginSuccess()
                return true
            }
        }
        return false
    }

    /**
     * Method send request only over NetworkProvider to load users cars from server.
     * @return true when provider is active, false otherwise.
     */
    fun sendNetworkGetCars(): Boolean {
        for (provider in activeCommunicators) {
            if (provider is NetworkProvider) {
                provider.getCars()
                return true
            }
        }
        return false
    }

    /**
     * Method send request only over NetworkProvider to create new car on server.
     * @return true when provider is active, false otherwise.
     */
    fun sendNetworkCreateCar(name: String): Boolean {
        for (provider in activeCommunicators) {
            if (provider is NetworkProvider) {
                provider.createCar(name)
                return true
            }
        }
        return false
    }
}