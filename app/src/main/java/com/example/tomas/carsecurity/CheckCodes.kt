package com.example.tomas.carsecurity

object CheckCodes {
    const val success: Byte = 1
    const val hardwareNotSupported: Byte = -1
    const val permissionDenied: Byte = -2
    const val notAllowed: Byte = -3
    const val invalidParameters: Byte = -4

    fun toString(checkCode: Byte): String {
        return when (checkCode) { // TODO use strings from resources
            hardwareNotSupported -> "hardware is not supported"
            permissionDenied -> "permission denied by user"
            notAllowed -> "sensor is disabled by user"
            invalidParameters -> "invalid setting"
            success -> "ok"
            else -> ""
        }
    }
}