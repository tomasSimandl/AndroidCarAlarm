package com.example.tomas.carsecurity.communication

import android.location.Location
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.UtilsEnum

class CommunicationManager(context: MyContext) {

    private val communicationContext = CommunicationContext(context.sharedPreferences, context.appContext)
    private val activeCommunicators: MutableSet<ICommunicationProvider> = HashSet()

    init {

        if(communicationContext.isProviderAllowed(SmsProvider::class.java.simpleName)){
            activeCommunicators.add(SmsProvider(communicationContext))
        }
    }

    fun destroy(){
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