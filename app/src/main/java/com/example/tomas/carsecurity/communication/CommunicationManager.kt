package com.example.tomas.carsecurity.communication

import android.content.SharedPreferences
import android.location.Location
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.network.NetworkProvider
import com.example.tomas.carsecurity.communication.sms.SmsProvider
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.UtilsEnum

class CommunicationManager(private val context: MyContext) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val activeCommunicators: MutableSet<ICommunicationProvider> = HashSet()

    init {
        tryInitializeProvider(SmsProvider(context.communicationContext))
        tryInitializeProvider(NetworkProvider(context.communicationContext))

        context.communicationContext.registerOnPreferenceChanged(this)
    }

    private fun tryInitializeProvider(provider: ICommunicationProvider) {
        if (provider.initialize()) {
            activeCommunicators.add(provider)
        }
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        //  TODO warning - run in main thread
        // TODO use all possible resources (phone number)
        if (key == context.appContext.getString(R.string.key_communication_sms_is_allowed)) {

            if(canRegisterProvider(activeCommunicators.find { it is SmsProvider }, sharedPreferences, key))
                tryInitializeProvider(SmsProvider(context.communicationContext))
        }

        if (key == context.appContext.getString(R.string.key_communication_network_is_allowed)) {

            if(canRegisterProvider(activeCommunicators.find { it is NetworkProvider }, sharedPreferences, key))
                tryInitializeProvider(NetworkProvider(context.communicationContext))
        }
    }

    /**
     * Method resolve if provider should be created. When provider is not allowed but already
     * exists, it is removed from active providers
     */
    private fun canRegisterProvider(provider: ICommunicationProvider?, sharedPreferences: SharedPreferences, key: String): Boolean{

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
        context.communicationContext.unregisterOnPreferenceChanged(this)
        activeCommunicators.forEach { it.destroy() }
        activeCommunicators.clear()
    }

    fun sendUtilSwitch(util: UtilsEnum, enabled: Boolean) {
        for (provider in activeCommunicators) {
            provider.sendUtilSwitch(util, enabled)
        }
    }

    fun sendEvent(messageType: MessageType, vararg args: Any) {
        for (provider in activeCommunicators) {
            provider.sendEvent(messageType, args)
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

}