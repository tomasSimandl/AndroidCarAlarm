package com.example.tomas.carsecurity.communication

import android.content.SharedPreferences
import android.util.Log
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.network.NetworkProvider
import com.example.tomas.carsecurity.communication.sms.SmsProvider
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.storage.Storage
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.utils.UtilsEnum

class CommunicationManager private constructor(private val communicationContext: CommunicationContext)
    : SharedPreferences.OnSharedPreferenceChangeListener {

    private val activeCommunicators: MutableSet<ICommunicationProvider> = HashSet()
    private val tag = "CommunicationManager"


    companion object {
        private var instance: CommunicationManager? = null

        fun getInstance(communicationContext: CommunicationContext): CommunicationManager {
            if (instance == null){
                Log.d("CommManager.instance", "Creating new instance of Communication manager.")
                instance = CommunicationManager(communicationContext)
            }
            return instance!!
        }
    }


    init {
        tryInitializeProvider(SmsProvider(communicationContext))
        tryInitializeProvider(NetworkProvider(communicationContext))

        communicationContext.registerOnPreferenceChanged(this)
    }

    private fun tryInitializeProvider(provider: ICommunicationProvider) {
        if (provider.initialize()) {
            activeCommunicators.add(provider)
        }
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        //  TODO warning - run in main thread
        // TODO use all possible resources (phone number)
        if (key == communicationContext.appContext.getString(R.string.key_communication_sms_is_allowed)) {

            if (canRegisterProvider(activeCommunicators.find { it is SmsProvider }, sharedPreferences, key))
                tryInitializeProvider(SmsProvider(communicationContext))
        }

        if (key == communicationContext.appContext.getString(R.string.key_communication_network_is_allowed)) {

            if (canRegisterProvider(activeCommunicators.find { it is NetworkProvider }, sharedPreferences, key))
                tryInitializeProvider(NetworkProvider(communicationContext))
        }

        if (key == communicationContext.appContext.getString(R.string.key_communication_network_is_user_login)) {
            if(!communicationContext.isLogin) {
                Thread (Runnable {
                    Log.d(tag, "User logout. Clearing database.")
                    Storage.getInstance(communicationContext.appContext).clearAllTables()
                }).start()
            }
        }
    }

    /**
     * Method resolve if provider should be created. When provider is not allowed but already
     * exists, it is removed from active providers
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


    fun destroy() {
        Log.d(tag, "Destroying")
        communicationContext.unregisterOnPreferenceChanged(this)
        activeCommunicators.forEach { it.destroy() }
        activeCommunicators.clear()
    }

    fun sendUtilSwitch(util: UtilsEnum, enabled: Boolean) {
        for (provider in activeCommunicators) {
            provider.sendUtilSwitch(util, enabled)
        }
    }

    fun sendEvent(messageType: MessageType, vararg args: String) {
        for (provider in activeCommunicators) {
            provider.sendEvent(messageType, *args)
        }
    }

    fun sendLocation(location: Location, isAlarm: Boolean, cache: Boolean = false) {
        for (provider in activeCommunicators) {
            provider.sendLocation(location, isAlarm, cache)
        }
    }

    fun sendStatus(communicatorHash: Int, battery: Float, isCharging: Boolean, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>) {
        for (provider in activeCommunicators) {
            if(provider::class.java.hashCode() == communicatorHash) {
                provider.sendStatus(battery, isCharging, powerSaveMode, utils)
                break
            }
        }
    }

    fun sendNetworkLogin(username: String, password: String){
        for (provider in activeCommunicators) {
            if(provider is NetworkProvider){
                provider.login(username, password)
                break
            }
        }
    }

    fun sendNetworkGetCars(){
        for (provider in activeCommunicators) {
            if (provider is NetworkProvider) {
                provider.getCars()
                break
            }
        }
    }

    fun sendNetworkCreateCar(name: String){
        for (provider in activeCommunicators) {
            if (provider is NetworkProvider) {
                provider.createCar(name)
                break
            }
        }
    }
}