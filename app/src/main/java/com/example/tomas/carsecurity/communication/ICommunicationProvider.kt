package com.example.tomas.carsecurity.communication

import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.utils.UtilsEnum

interface ICommunicationProvider {

    // initialization status can be changed only via methods initialize and destroy called
    // from [CommunicationManager]
    fun initialize(): Boolean
    fun destroy()
    fun isInitialize(): Boolean

    fun sendEvent(messageType: MessageType, vararg args: String): Boolean
    fun sendUtilSwitch(utilsEnum: UtilsEnum, enabled: Boolean): Boolean
    fun sendLocation(location: Location, isAlarm: Boolean, cache: Boolean = false): Boolean
    fun sendStatus(battery: Float, isCharging: Boolean, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>): Boolean
    fun sendRoute(localRouteId: Int): Boolean
}