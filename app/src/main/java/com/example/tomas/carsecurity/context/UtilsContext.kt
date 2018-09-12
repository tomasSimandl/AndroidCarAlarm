package com.example.tomas.carsecurity.context

import android.content.Context
import com.example.tomas.carsecurity.R

/**
 * Context contains data which are which are connected with utils package class and they are stored
 * in shared preferences or in resources.
 */
class UtilsContext(appContext: Context): BaseContext(appContext) {


    // ======================================= TRACKER UTIL ========================================

    /** Returns new location ignore distance in meters. Value is taken from shared preferences or it is used default value. */
    val ignoreDistance
        get() = getInt(R.string.key_tool_tracker_ignore_distance, R.integer.default_tracker_ignore_distance)

    /** Returns not moving timeout in milliseconds. Value is taken from shared preferences or it is used default value. */
    val timeout
        get() = getInt(R.string.key_tool_tracker_timeout, R.integer.default_tracker_timeout)


    // ======================================== ALARM UTIL =========================================

    /**
     * Returns default interval between two detections in which alarm will be triggered.
     * Value is taken from shared preferences or it is used default value.
     */
    val alertAlarmInterval
        get() = getInt(R.string.key_tool_alarm_alert_interval, R.integer.default_alarm_alert_interval)

    /**
     * Returns default interval for which is detection ignored after alarm activation.
     * Value is taken from shared preferences or it is used default value.
     */
    val startAlarmInterval
        get() = getInt(R.string.key_tool_alarm_start_interval, R.integer.default_alarm_start_interval)
}


