package com.example.tomas.carsecurity.communication

import android.location.Location
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.UtilsEnum

class CommunicationManager(context: MyContext) {

    private val activeCommunicators: MutableSet<ICommunicationProvider> = HashSet()

    init {
        for (provider in context.smsProviderContext.activeProviders){
            when (provider) {
                SmsProvider::class.java.simpleName -> activeCommunicators.add(SmsProvider(context))
                "InternetProvider" -> UnsupportedOperationException("Not implemented")
            }
        }
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

    fun sendLocation(location: Location) {
        for (provider in activeCommunicators) {
            provider.sendLocation(location)
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