package com.example.tomas.carsecurity.communication

import android.content.SharedPreferences
import android.location.Location
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.sms.SmsProvider
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.UtilsEnum

class CommunicationManager(private val context: MyContext): SharedPreferences.OnSharedPreferenceChangeListener {

    private val activeCommunicators: MutableSet<ICommunicationProvider> = HashSet()

    init {
        val provider = SmsProvider(context.communicationContext)
        if (provider.initialize()) {
            activeCommunicators.add(provider)
        }
        context.communicationContext.registerOnPreferenceChanged(this)
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        //  TODO warning - run in main thread
        // TODO use all possible resources (phone number)
        if (key == context.appContext.getString(R.string.key_communication_sms_is_allowed)) {

            val provider = activeCommunicators.find { it is SmsProvider }

            if (sharedPreferences.getBoolean(key, false)) {
                // new value is true
                if (provider == null) {
                    // provider is not registered yet
                    val newProvider = SmsProvider(context.communicationContext)
                    if (newProvider.initialize()) {
                        activeCommunicators.add(newProvider)
                    }
                }
            } else {
                //new value is false
                if (provider != null){
                    provider.destroy()
                    activeCommunicators.remove(provider)
                }
            }
        }
    }


    fun destroy(){
        context.communicationContext.unregisterOnPreferenceChanged(this)
        activeCommunicators.forEach { it.destroy() }
        activeCommunicators.clear()
    }


    fun sendUtilSwitch(util: UtilsEnum, enabled: Boolean) {
        for (provider in activeCommunicators) {
            provider.sendUtilSwitch(util, enabled)
        }
    }

    fun sendAlarm() {
        for (provider in activeCommunicators) {
            provider.sendAlarm()
        }
    }

    fun sendLocation(location: Location, isAlarm: Boolean) {
        for (provider in activeCommunicators) {
            provider.sendLocation(location, isAlarm)
        }
    }

    fun sendBatteryWarn(capacity: Int) {
        for (provider in activeCommunicators) {
            provider.sendBatteryWarn(capacity)
        }
    }

    fun sendPowerConnected(capacity: Int) {
        for (provider in activeCommunicators) {
            provider.sendPowerConnected(capacity)
        }
    }

    fun sendPowerDisconnected(capacity: Int) {
        for (provider in activeCommunicators) {
            provider.sendPowerDisconnected(capacity)
        }
    }

    fun sendStatus(battery: Int, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>){
        for (provider in activeCommunicators) {
            provider.sendStatus(battery, powerSaveMode, utils)
        }
    }

}