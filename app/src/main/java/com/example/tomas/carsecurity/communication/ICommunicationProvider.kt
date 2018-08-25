package com.example.tomas.carsecurity.communication

import com.example.tomas.carsecurity.utils.UtilsEnum

interface ICommunicationProvider {

    fun sendMessage(text: String): Boolean


    fun sendUtilSwitch(utilsEnum: UtilsEnum, enabled: Boolean): Boolean
}