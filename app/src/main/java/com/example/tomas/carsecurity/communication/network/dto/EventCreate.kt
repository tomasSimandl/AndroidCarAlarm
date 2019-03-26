package com.example.tomas.carsecurity.communication.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Class represents Event object which is transfer over network to create event on server.
 */
data class EventCreate(

        /** Id of event type defined by server */
        @SerializedName("event_type_id")
        val eventTypeId: Long,

        /** Time of event in milliseconds since epoch. */
        val time: Long,

        /** Id of car which is associated with this event. */
        @SerializedName("car_id")
        val carId: Long,

        /** Information note for this event. */
        val note: String = ""
)