package com.example.tomas.carsecurity.communication.network.dto

data class EventCreate(

        val eventTypeId: Long,

        val time: Long,

        val carId: Long,

        val note: String = ""
)