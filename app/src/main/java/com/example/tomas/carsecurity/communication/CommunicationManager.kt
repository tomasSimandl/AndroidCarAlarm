package com.example.tomas.carsecurity.communication

import android.content.IntentFilter
import android.location.Location
import android.support.v4.content.LocalBroadcastManager
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.UtilsEnum

class CommunicationManager(context: MyContext) {

    private val activeCommunicators: MutableSet<ICommunicationProvider> = HashSet()

    private val communicationContext = CommunicationContext(context.sharedPreferences, context.appContext)

    init {
        for (provider in communicationContext.activeProviders){
            when (provider) {
                SmsProvider::class.java.simpleName -> {
                    activeCommunicators.add(SmsProvider(communicationContext))
                    val intentFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
                    context.appContext.registerReceiver(SmsBroadcastReceiver(communicationContext), intentFilter)
                }
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