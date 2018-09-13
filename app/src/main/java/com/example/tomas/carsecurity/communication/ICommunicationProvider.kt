package com.example.tomas.carsecurity.communication

import android.location.Location
import com.example.tomas.carsecurity.utils.UtilsEnum

interface ICommunicationProvider {

    // initialization status can be changed only via methods initialize and destroy called
    // from [CommunicationManager]
    fun initialize(): Boolean
    fun destroy()
    fun isInitialize(): Boolean

    fun sendMessage(text: String): Boolean


    fun sendUtilSwitch(utilsEnum: UtilsEnum, enabled: Boolean): Boolean
    fun sendAlarm(): Boolean
    fun sendLocation(location: Location, isAlarm: Boolean): Boolean
    fun sendBatteryWarn(capacity: Int): Boolean
    fun sendPowerConnected(capacity: Int): Boolean
    fun sendPowerDisconnected(capacity: Int): Boolean
    fun sendStatus(battery: Int, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>): Boolean
}