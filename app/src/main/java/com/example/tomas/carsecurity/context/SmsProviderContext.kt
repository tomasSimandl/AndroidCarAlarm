package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R

class SmsProviderContext(private val sharedPreferences: SharedPreferences, private val context: Context) {

    /** Returns number of contact person. Return value from sharedPreferences or empty string. */
    val phoneNumber: String
        get() = sharedPreferences.getString(context.getString(R.string.key_contact_phone_number), "")
}
