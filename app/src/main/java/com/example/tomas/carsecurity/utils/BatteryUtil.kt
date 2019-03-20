package com.example.tomas.carsecurity.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object BatteryUtil {

    fun getBatteryStatus(context: Context): Pair<Float, Boolean> {

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }

        if (batteryStatus != null){
            return Pair(
                    batteryPct(batteryStatus),
                    batteryIsCharging(batteryStatus))
        }

        return Pair(-1f, false)
    }

    fun batteryIsCharging(intent: Intent): Boolean{
        val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        return status == BatteryManager.BATTERY_STATUS_CHARGING  ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun batteryPct(intent: Intent): Float {
        val batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 1).toFloat()
        return  batteryLevel / batteryScale
    }
}