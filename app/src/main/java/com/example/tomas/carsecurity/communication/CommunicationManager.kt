package com.example.tomas.carsecurity.communication

import com.example.tomas.carsecurity.utils.UtilsEnum

class CommunicationManager {

    private val activeCommunicators: MutableSet<ICommunicationProvider> = HashSet()

    fun utilSwitch(util: UtilsEnum, enabled: Boolean) {
        for (provider in activeCommunicators) {
            provider.sendUtilSwitch(util, enabled)
        }
    }


}