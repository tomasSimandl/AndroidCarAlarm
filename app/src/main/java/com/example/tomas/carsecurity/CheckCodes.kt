package com.example.tomas.carsecurity

import android.content.Context

/**
 * Representation of [CheckObjByte] result codes.
 */
object CheckCodes {
    /** Check was success and sensor can be enabled */
    const val success: Byte = 1
    /** Sensor can not be enabled. Sensor is not supported by HW */
    const val hardwareNotSupported: Byte = -1
    /** Sensor can not be enabled. Not enough permissions */
    const val permissionDenied: Byte = -2
    /** Sensor can not be enabled. Not allowed by user in settings */
    const val notAllowed: Byte = -3
    /** Sensor can not be enabled. Invalid configuration in settings */
    const val invalidParameters: Byte = -4

    /**
     * Method returns information messages for user based on input [checkCode].
     *
     * @param checkCode of which message will be returned.
     * @param context used for getting messages from resources.
     * @return information messages with problem or 'OK' on success.
     */
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