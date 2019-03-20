package com.example.tomas.carsecurity.communication.network.dto

import com.example.tomas.carsecurity.tools.ToolsEnum
import com.google.gson.annotations.SerializedName

/**
 * Class represents Status object which is transfer over network to create status on server.
 */
data class StatusCreate(

        /** Battery level status 0 = discharged, 1 = fully charged */
        val battery: Float,

        /** Indication if device is charging now */
        @SerializedName("is_charging")
        val isCharging: Boolean,

        /** Indication if device is in power save mode */
        @SerializedName("is_power_save_mode")
        val powerSaveMode: Boolean,

        /** Map with activated tools */
        val tools: Map<ToolsEnum, Boolean>,

        /** Time when status was created in milliseconds since epoch. */
        val time: Long,

        /** Car id of car which is associated with this status. */
        @SerializedName("car_id")
        val carId: Long
)