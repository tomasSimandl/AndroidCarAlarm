package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R

open class BaseContext(val appContext: Context) {

    /** Contains private shared preferences which are shared across application. */
    private val sharedPreferences = appContext.getSharedPreferences(
            appContext.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE)

    protected fun getInt(resourceKeyId: Int, defValueId: Int): Int {
        return sharedPreferences.getInt(
                appContext.getString(resourceKeyId),
                appContext.resources.getInteger(defValueId))
    }

    protected fun getString(resourceKeyId: Int, defValueId: Int): String {
        return sharedPreferences.getString(
                appContext.getString(resourceKeyId),
                appContext.resources.getString(defValueId)) ?: ""
    }

    protected fun getBoolean(resourceKeyId: Int, defValueId: Int): Boolean {
        return sharedPreferences.getBoolean(
                appContext.getString(resourceKeyId),
                appContext.resources.getBoolean(defValueId))
    }

    protected fun getStringSet(resourceKeyId: Int, defValue: Set<String>?): Set<String>? {
        return sharedPreferences.getStringSet(appContext.getString(resourceKeyId), defValue)
    }

    fun registerOnPreferenceChanged(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnPreferenceChanged(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}