package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R

class SmsProviderContext(private val sharedPreferences: SharedPreferences, val context: Context) {

    private val defActiveProviders = context.resources.getStringArray(R.array.default_communication_active_providers).toHashSet()

    val activeProviders: Set<String>
        get() = sharedPreferences.getStringSet(context.getString(R.string.key_communication_active_providers), defActiveProviders)

    /** Returns number of contact person. Return value from sharedPreferences or empty string. */
    val phoneNumber: String
        get() = sharedPreferences.getString(context.getString(R.string.key_contact_phone_number), "")
}
