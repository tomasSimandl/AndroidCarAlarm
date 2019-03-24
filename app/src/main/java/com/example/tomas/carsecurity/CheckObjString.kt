package com.example.tomas.carsecurity

import android.content.Context

/**
 * Interface used in Tools for checking if tool can be enabled.
 */
interface CheckObjString {

    /**
     * Method checks if there is some restriction which prevents of tool activation.
     *
     * @param context is application context
     * @param skipAllow indicates if should be check tools allow attribute set by user.
     * @return Error message when there is some problem or empty string when tool can be enabled.
     */
    fun check(context: Context, skipAllow: Boolean): String
}