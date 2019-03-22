package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R

/**
 * Class represents base class for specific contexts. Contains mainly general methods for access values in
 * [SharedPreferences].
 *
 * @param appContext is application context which will be used across whole application.
 */
open class BaseContext(val appContext: Context) {

    /** Enum fo indication if application is in power save mode or not. */
    protected enum class Mode {
        Normal, PowerSaveMode
    }

    protected val mode
        /** @return indication if application is in power save mode. */
        get() = Mode.valueOf(sharedPreferences.getString(appContext.getString(R.string.key_tool_battery_mode), Mode.Normal.name)
                ?: Mode.Normal.name)

    /** Private shared preferences which are shared across whole application. */
    protected val sharedPreferences: SharedPreferences = appContext.getSharedPreferences(
            appContext.getString(R.string.preference_file_key), Context.MODE_PRIVATE)

    /**
     * Method returns integer value which is loaded from [sharedPreferences]. When value is not presents resource
     * specified by [defValueId] is returned.
     *
     * @param resourceKeyId is resource id of string which identifies value in [sharedPreferences]
     * @param defValueId is resource id of integer which is load from resources and return as default value
     * @return loaded value from [sharedPreferences] or default value
     */
    protected fun getInt(resourceKeyId: Int, defValueId: Int): Int {
        return sharedPreferences.getInt(
                appContext.getString(resourceKeyId),
                appContext.resources.getInteger(defValueId))
    }

    /**
     * Method returns string value which is loaded from [sharedPreferences]. When value is not presents resource
     * specified by [defValueId] is returned.
     *
     * @param resourceKeyId is resource id of string which identifies value in [sharedPreferences]
     * @param defValueId is resource id of string which is load from resources and return as a default value
     * @return loaded value from [sharedPreferences] or default value
     */
    protected fun getString(resourceKeyId: Int, defValueId: Int): String {
        return sharedPreferences.getString(
                appContext.getString(resourceKeyId),
                appContext.resources.getString(defValueId)) ?: ""
    }

    /**
     * Method store [value] to shared preferences under string key specified by string resource id [resourceKeyId].
     *
     * @param resourceKeyId is id to string resource which identifies value in [sharedPreferences]
     * @param value is new value which will be inserted to [sharedPreferences]
     */
    protected fun putString(resourceKeyId: Int, value: String) {
        sharedPreferences.edit()
                .putString(appContext.getString(resourceKeyId), value)
                .apply()
    }

    /**
     * Method returns boolean value which is loaded from [sharedPreferences]. When value is not presents resource
     * specified by [defValueId] is returned.
     *
     * @param resourceKeyId is resource id of string which identifies value in [sharedPreferences]
     * @param defValueId is resource id of boolean which is load from resources and return as default value
     * @return loaded value from [sharedPreferences] or default value
     */
    protected fun getBoolean(resourceKeyId: Int, defValueId: Int): Boolean {
        return sharedPreferences.getBoolean(
                appContext.getString(resourceKeyId),
                appContext.resources.getBoolean(defValueId))
    }

    /**
     * Method store [value] to shared preferences under string key specified by string resource id [resourceKeyId].
     *
     * @param resourceKeyId is id to string resource which identifies value in [sharedPreferences]
     * @param value is new value which will be inserted to [sharedPreferences]
     */
    protected fun putBoolean(resourceKeyId: Int, value: Boolean) {
        sharedPreferences.edit()
                .putBoolean(appContext.getString(resourceKeyId), value)
                .apply()
    }

    /**
     * Method return string set which is loaded from [sharedPreferences]. When value is not present, [defValue] is
     * returned.
     *
     * @param resourceKeyId is id to string resource which identifies value is [sharedPreferences]
     * @param defValue is returned default value when value is not present in [sharedPreferences]
     * @return value from [sharedPreferences] or [defValue].
     */
    protected fun getStringSet(resourceKeyId: Int, defValue: Set<String>?): Set<String>? {
        return sharedPreferences.getStringSet(appContext.getString(resourceKeyId), defValue)
    }

    /**
     * Method register given [listener] to listen on changes in [sharedPreferences].
     *
     * @param listener which should be registered to listen on [sharedPreferences] changes.
     */
    fun registerOnPreferenceChanged(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Method unregister given [listener] from listening on [sharedPreferences] changes.
     * @param listener which should be unregistered.
     */
    fun unregisterOnPreferenceChanged(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}