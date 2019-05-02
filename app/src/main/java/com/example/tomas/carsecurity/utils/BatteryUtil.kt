package com.example.tomas.carsecurity.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Object used for getting actual battery status.
 */
object BatteryUtil {

    /**
     * Method get actual battery status from system and return it in [Pair]
     *
     * @param context is application context
     * @return [Pair] where first parameter is battery level in range <0,1> and second parameter indicates if
     *          device is connected to external source of power.
     */
    fun getBatteryStatus(context: Context): Pair<Float, Boolean> {

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }

        if (batteryStatus != null) {
            return Pair(
                    batteryPct(batteryStatus),
                    batteryIsCharging(batteryStatus))
        }

        return Pair(-1f, false)
    }

    /**
     * Method get from input [intent] indication if device is connected to external source of power.
     *
     * @param intent is [Intent] which contains battery info.
     * @return true when device is connected to external source of power.
     */
    private fun batteryIsCharging(intent: Intent): Boolean {
        val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * Method get from input [intent] actual level of battery. Level is in interval <0,1>.
     *
     * @param intent is [Intent] which contains battery info.
     * @return number from interval <0,1> which represents actual battery level.
     */
    private fun batteryPct(intent: Intent): Float {
        val batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 1).toFloat()
        return batteryLevel / batteryScale
    }
}