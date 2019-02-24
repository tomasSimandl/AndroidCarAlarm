package com.example.tomas.carsecurity.communication.network.dto

import com.google.gson.annotations.SerializedName

data class EventCreate(

        @SerializedName("event_type_id")
        val eventTypeId: Long,

        val time: Long,

        val carId: Long,

        val note: String = ""
)