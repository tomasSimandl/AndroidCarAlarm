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
    private val defAlertAlarmInterval :Long = context.resources.getInteger(R.integer.default_alarm_alert_interval).toLong()

    /** Contains default interval for which is detection ignored after alarm activation. */
    private val defStartAlarmInterval :Long = context.resources.getInteger(R.integer.default_alarm_start_interval).toLong()

    /**
     * Returns default interval between two detections in which alarm will be triggered.
     * Value is taken from shared preferences or it is used default value.
     */
    val alertAlarmInterval
        get() = sharedPreferences.getLong(context.getString(R.string.key_alarm_alert_interval), defAlertAlarmInterval)

    /**
     * Returns default interval for which is detection ignored after alarm activation.
     * Value is taken from shared preferences or it is used default value.
     */
    val startAlarmInterval
        get() = sharedPreferences.getLong(context.getString(R.string.key_alarm_start_interval), defStartAlarmInterval)

}


