package com.example.tomas.carsecurity.context

import android.content.Context
import android.content.SharedPreferences
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.utils.Alarm

/**
 * Context contains data which are used in [Alarm] class and they are stored in
 * shared preferences or in resources.
 */
class AlarmContext(private val sharedPreferences: SharedPreferences, private val context: Context) {

    /** Contains default interval between two detections in which alarm will be triggered. Value is taken from resources. */
    private val defAlertAlarmInterval :Int = context.resources.getInteger(R.integer.default_alarm_alert_interval)

    /** Contains default interval for which is detection ignored after alarm activation. */
    private val defStartAlarmInterval :Int = context.resources.getInteger(R.integer.default_alarm_start_interval)

    /**
     * Returns default interval between two detections in which alarm will be triggered.
     * Value is taken from shared preferences or it is used default value.
     */
    val alertAlarmInterval
        get() = sharedPreferences.getInt(context.getString(R.string.key_tool_alarm_alert_interval), defAlertAlarmInterval)

    /**
     * Returns default interval for which is detection ignored after alarm activation.
     * Value is taken from shared preferences or it is used default value.
     */
    val startAlarmInterval
        get() = sharedPreferences.getInt(context.getString(R.string.key_tool_alarm_start_interval), defStartAlarmInterval)

}


