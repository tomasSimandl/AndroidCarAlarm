package com.example.tomas.carsecurity.communication

interface ICommunicationProvider {

    fun sendMessage(text: String): Boolean
}