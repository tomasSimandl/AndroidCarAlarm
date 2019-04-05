package com.example.tomas.carsecurity.context

import android.content.Context
import com.example.tomas.carsecurity.R

/**
 * Context contains data which are used in tools package and they are stored in shared preferences or in resources.
 *
 * @param appContext is application context which will be shared across whole application.
 */
class ToolsContext(appContext: Context) : BaseContext(appContext) {

    /** Method enables power save mode. */
    fun enablePowerSaveMode() {
        switchBatteryMode(Mode.PowerSaveMode)
    }

    /** Method disabled power save mode */
    fun disablePowerSaveMode() {
        switchBatteryMode(Mode.Normal)
    }

    /**
     * Method change value in [sharedPreferences] which indicates if application is in power save mode.
     *
     * @param mode is new power save mode which will be stored in [sharedPreferences].
     */
    private fun switchBatteryMode(mode: Mode) {
        sharedPreferences.edit().putString(appContext.getString(R.string.key_tool_battery_mode), mode.name).apply()
    }

    /**
     * Indication if application is in power save mode.
     */
    val isPowerSaveMode
        get() = sharedPreferences.getString(appContext.getString(R.string.key_tool_battery_mode), "") == Mode.PowerSaveMode.name

    /**
     * Indication if application can be switched to power save mode.
     */
    val isBatteryModeAllowed
        get() = getBoolean(R.string.key_tool_battery_mode_is_allowed, R.bool.default_tool_battery_mode_is_allowed)

    /**
     * Level of battery which is taken as critical => application will be switched to power save mode.
     */
    val batteryCriticalLevel
        get() = getInt(R.string.key_tool_battery_critical_level, R.integer.default_tool_battery_critical_level)

    // ======================================= TRACKER UTIL ========================================

    /** Returns if tracker is allowed by user */
    val isTrackerAllowed
        get() = getBoolean(R.string.key_tool_tracker_is_allowed, R.bool.default_tool_tracker_is_allowed)

    /** Returns new location ignore distance in meters. Value is taken from shared preferences or it is used default value. */
    val ignoreDistance
        get() = getInt(R.string.key_tool_tracker_ignore_distance, R.integer.default_tool_tracker_ignore_distance)

    /** Returns not moving timeout in milliseconds. Value is taken from shared preferences or it is used default value. */
    val timeout
        get() = getInt(R.string.key_tool_tracker_timeout, R.integer.default_tool_tracker_timeout).toLong() * 1000

    /** Length af actual recorder route in meters. */
    var actualLength: Float
        get() = getFloat(R.string.key_tool_tracker_actual_length, -1f)
        set(value) = putFloat(R.string.key_tool_tracker_actual_length, value)

    // ======================================== ALARM UTIL =========================================

    /** Returns if alarm is allowed by user */
    val isAlarmAllowed
        get() = getBoolean(R.string.key_tool_alarm_is_allowed, R.bool.default_tool_alarm_is_allowed)

    /**
     * Returns interval between two detections in which alarm will be triggered.
     * Value is taken from shared preferences or it is used default value.
     */
    val alertAlarmInterval
        get() = getInt(R.string.key_tool_alarm_alert_interval, R.integer.default_tool_alarm_alert_interval) * 1000

    /**
     * Returns interval for which is detection ignored after alarm activation.
     * Value is taken from shared preferences or it is used default value.
     */
    val startAlarmInterval
        get() = getInt(R.string.key_tool_alarm_start_interval, R.integer.default_tool_alarm_start_interval) * 1000

    /**
     * Returns interval in which will be send position sms messages to user when alarm is triggered.
     */
    val sendLocationInterval
        get() = when (mode) {
            Mode.Normal -> getInt(R.string.key_tool_alarm_send_location_interval, R.integer.default_tool_alarm_send_location_interval) * 1000
            Mode.PowerSaveMode -> appContext.resources.getInteger(R.integer.battery_save_mode_tool_alarm_send_location_interval) * 1000
        }

    /**
     * Returns interval indicates limit to turn off location sensor when update intervals are slow.
     */
    val disableSendLocationInterval
        get() = appContext.resources.getInteger(R.integer.default_tool_alarm_disable_send_location_interval)

    /**
     * Indicates if can be used siren. Decision is created on fact that application is in power save mode or not.
     */
    val isSirenAllow
        get() = when (mode) {
            Mode.Normal -> getBoolean(R.string.key_tool_alarm_siren_is_allowed, R.bool.default_tool_alarm_siren_is_allowed)
            Mode.PowerSaveMode -> appContext.resources.getBoolean(R.bool.battery_save_mode_tool_alarm_siren_is_allowed)
        }

    /**
     * Indicates if phone call on alarm is allowed.
     */
    val isCallAllow
        get() = getBoolean(R.string.key_tool_alarm_is_call_allowed, R.bool.default_tool_alarm_is_call_allowed)
}


