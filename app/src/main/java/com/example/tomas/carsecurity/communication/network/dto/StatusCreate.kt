package com.example.tomas.carsecurity.communication.network.dto

import com.example.tomas.carsecurity.utils.UtilsEnum
import com.google.gson.annotations.SerializedName

data class StatusCreate(

        val battery: Float,

        @SerializedName("is_charging")
        val isCharging: Boolean,

        @SerializedName("is_power_save_mode")
        val powerSaveMode: Boolean,

        val utils: Map<UtilsEnum, Boolean>,

        val time: Long,

        @SerializedName("car_id")
        val carId: Long
)