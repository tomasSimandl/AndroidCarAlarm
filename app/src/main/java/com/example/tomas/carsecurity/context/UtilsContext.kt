package com.example.tomas.carsecurity.context

import android.content.Context
import com.example.tomas.carsecurity.R

/**
 * Context contains data which are which are connected with tools package class and they are stored
 * in shared preferences or in resources.
 */
class UtilsContext(appContext: Context): BaseContext(appContext) {

    fun enablePowerSaveMode(){
        switchBatteryMode(Mode.PowerSaveMode)
    }

    fun disablePowerSaveMode(){
        switchBatteryMode(Mode.Normal)
    }

    private fun switchBatteryMode(mode: Mode){
        sharedPreferences.edit().putString(appContext.getString(R.string.key_tool_battery_mode), mode.name).apply()
    }

    val isPowerSaveMode
        get() = sharedPreferences.getString(appContext.getString(R.string.key_tool_battery_mode), "") == Mode.PowerSaveMode.name

    val isBatteryModeAllowed
        get() = getBoolean(R.string.key_tool_battery_mode_is_allowed, R.bool.default_tool_battery_mode_is_allowed)

    val batteryCriticalLevel
        get() = getInt(R.string.key_tool_battery_critical_level, R.integer.default_tool_battery_critical_level)

    // ======================================= TRACKER UTIL ========================================

    /** Returns if tracker is allowed by user */
    val isTrackerAllowed
        get() = when(mode){
            Mode.Normal -> getBoolean(R.string.key_tool_tracker_is_allowed, R.bool.default_tool_tracker_is_allowed)
            Mode.PowerSaveMode -> appContext.resources.getBoolean(R.bool.battery_save_mode_tool_tracker_is_allowed)
        }

    /** Returns new location ignore distance in meters. Value is taken from shared preferences or it is used default value. */
    val ignoreDistance
        get() = getInt(R.string.key_tool_tracker_ignore_distance, R.integer.default_tool_tracker_ignore_distance)

    /** Returns not moving timeout in milliseconds. Value is taken from shared preferences or it is used default value. */
    val timeout
        get() = getInt(R.string.key_tool_tracker_timeout, R.integer.default_tool_tracker_timeout) * 1000


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
        get() = when(mode){
            Mode.Normal -> getInt(R.string.key_tool_alarm_send_location_interval, R.integer.default_tool_alarm_send_location_interval) * 1000
            Mode.PowerSaveMode -> appContext.resources.getInteger(R.integer.battery_save_mode_tool_alarm_send_location_interval) * 1000
        }

    val disableSendLocationInterval
        get() = appContext.resources.getInteger(R.integer.default_tool_alarm_disable_send_location_interval)


    val isSirenAllow
        get() = when(mode) {
            Mode.Normal -> getBoolean(R.string.key_tool_alarm_siren_is_allowed, R.bool.default_tool_alarm_siren_is_allowed)
            Mode.PowerSaveMode -> appContext.resources.getBoolean(R.bool.battery_save_mode_tool_alarm_siren_is_allowed)
        }


    val isCallAllow
        get() = getBoolean(R.string.key_tool_alarm_is_call_allowed, R.bool.default_tool_alarm_is_call_allowed)
}


