package com.example.tomas.carsecurity.communication

class CommunicationManager {

    enum class Priority{
        LOW, MEDIUM, HIGH
    }

    private val activeComunicators = HashMap<ICommunicationProvider, Priority>()

    fun sendMessage(priority: Priority, text: String){
        // TODO empty body
    }

}