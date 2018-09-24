package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R


open class BaseContext(val appContext: Context) {

    protected enum class Mode {
        Normal, PowerSaveMode
    }

    protected val mode
        get() = Mode.valueOf(sharedPreferences.getString(appContext.getString(R.string.key_tool_battery_mode), Mode.Normal.name) ?: Mode.Normal.name)

    /** Contains private shared preferences which are shared across application. */
    protected val sharedPreferences = appContext.getSharedPreferences(
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