package com.example.tomas.carsecurity.communication

import android.content.SharedPreferences
import android.location.Location
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.UtilsEnum

class CommunicationManager(context: MyContext): SharedPreferences.OnSharedPreferenceChangeListener {

    private val communicationContext = CommunicationContext(context.appContext)
    private val activeCommunicators: MutableSet<ICommunicationProvider> = HashSet()

    init {
        val provider = SmsProvider(communicationContext)
        if (provider.initialize()) {
            activeCommunicators.add(provider)
        }
        communicationContext.registerOnPreferenceChanged(this)
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {

        if (key == communicationContext.appContext.getString(R.string.key_communication_sms_is_allowed)) {

            val provider = activeCommunicators.find { it is SmsProvider }

            if (sharedPreferences.getBoolean(key, false)) {
                // new value is true
                if (provider == null) {
                    // provider is not registered yet
                    val newProvider = SmsProvider(communicationContext)
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
        communicationContext.unregisterOnPreferenceChanged(this)
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

    fun sendStatus(battery: Int, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>){
        for (provider in activeCommunicators) {
            provider.sendStatus(battery, powerSaveMode, utils)
        }
    }

}