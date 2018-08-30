package com.example.tomas.carsecurity.communication

import android.location.Location
import com.example.tomas.carsecurity.utils.UtilsEnum

interface ICommunicationProvider {

    fun destroy()

    fun sendMessage(text: String): Boolean


    fun sendUtilSwitch(utilsEnum: UtilsEnum, enabled: Boolean): Boolean
    fun sendAlarm(): Boolean
    fun sendLocation(location: Location): Boolean
    fun sendBatteryWarn(capacity: Int): Boolean
    fun sendStatus(battery: Int, powerSaveMode: Boolean, utils: Map<UtilsEnum, Boolean>): Boolean
}