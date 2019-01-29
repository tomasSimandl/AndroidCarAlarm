package com.example.tomas.carsecurity.communication.network.dto

import java.text.SimpleDateFormat
import java.util.*

data class EventCreate(

        val eventTypeId: Long,

        val time: String,

        val carId: Long,

        val note: String = ""
) {
    constructor(eventTypeId: Long, time: Date, carId: Long, note: String):
            this(eventTypeId, SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S").format(time), carId, note)
}