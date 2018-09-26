package com.example.tomas.carsecurity

import android.content.Context

object CheckCodes {
    const val success: Byte = 1
    const val hardwareNotSupported: Byte = -1
    const val permissionDenied: Byte = -2
    const val notAllowed: Byte = -3
    const val invalidParameters: Byte = -4

    fun toString(checkCode: Byte, context: Context): String {
        return when (checkCode) {
            hardwareNotSupported -> context.getString(R.string.error_check_codes_hw_not_supported)
            permissionDenied -> context.getString(R.string.error_check_codes_not_permitted)
            notAllowed -> context.getString(R.string.error_check_codes_not_allowed)
            invalidParameters -> context.getString(R.string.error_check_codes_invalid_params)
            success -> context.getString(R.string.error_check_codes_success)
            else -> ""
        }
    }
}