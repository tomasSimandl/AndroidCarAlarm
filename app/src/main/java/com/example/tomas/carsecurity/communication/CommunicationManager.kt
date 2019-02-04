package com.example.tomas.carsecurity.communication

import android.content.SharedPreferences
import android.util.Log
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.network.NetworkProvider
import com.example.tomas.carsecurity.communication.sms.SmsProvider
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.utils.UtilsEnum

class CommunicationManager(private val communicationContext: CommunicationContext)
    : SharedPreferences.OnSharedPreferenceChangeListener {

    private val activeCommunicators: MutableSet<ICommunicationProvider> = HashSet()
    private val tag = "CommunicationManager"


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

    fun sendStatus(battery: Int, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>) {
        for (provider in activeCommunicators) {
            provider.sendStatus(battery, powerSaveMode, utils)
        }
    }

    fun sendRoute(localRouteId: Int) {
        for (provider in activeCommunicators) {
            provider.sendRoute(localRouteId)
        }
    }

}