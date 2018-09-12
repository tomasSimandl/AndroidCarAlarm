package com.example.tomas.carsecurity.context

import android.content.Context
import com.example.tomas.carsecurity.R

/**
 * Context contains data which are which are connected with utils package class and they are stored
 * in shared preferences or in resources.
 */
class UtilsContext(appContext: Context): BaseContext(appContext) {


    // ======================================= TRACKER UTIL ========================================

    /** Returns if tracker is allowed by user */
    val isTrackerAllowed
        get() = getBoolean(R.string.key_tool_tracker_is_allowed, R.bool.default_tool_tracker_is_allowed)

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
        get() = getInt(R.string.key_tool_alarm_send_location_interval, R.integer.default_tool_alarm_send_location_interval) * 1000

    val isSirenAllow
        get() = getBoolean(R.string.key_tool_alarm_siren_is_allow, R.bool.default_tool_alarm_siren_is_allow)
}


