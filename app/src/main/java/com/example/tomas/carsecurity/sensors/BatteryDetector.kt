package com.example.tomas.carsecurity.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.BatteryUtil

class BatteryDetector (private val context: MyContext): GeneralObservable() {

    private val tag = "BatteryDetector"

    private var enabled = false

    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, """Battery action: ${intent.action}""")

            notifyObservers(intent.action, intent)
        }
    }

    private val powerReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, """Power action: ${intent.action}""")

            val batteryStatus = BatteryUtil.getBatteryStatus(context)

            if(batteryStatus.first != -1f) {
                setChanged()
                notifyObservers(Triple(intent.action, batteryStatus.second, batteryStatus.first))
            }
        }
    }

    override fun canEnable(): Boolean {
        return true
    }

    private fun notifyObservers(action: String?, intent: Intent){

        if(action == null) return

        val isCharging: Boolean = BatteryUtil.batteryIsCharging(intent)
        val batteryPct: Float = BatteryUtil.batteryPct(intent)

        setChanged()
        notifyObservers(Triple(action, isCharging ,batteryPct))
    }


    override fun disable() {
        if (enabled) {
            enabled = false
            context.appContext.unregisterReceiver(batteryReceiver)
            context.appContext.unregisterReceiver(powerReceiver)
            Log.d(tag, "Detector is disabled")
        }
    }

    override fun enable() {
        if (!enabled) {
            enabled = true

            context.appContext.registerReceiver(batteryReceiver, IntentFilter("android.intent.action.BATTERY_CHANGED"))
            context.appContext.registerReceiver(powerReceiver, IntentFilter("android.intent.action.ACTION_POWER_CONNECTED"))
            context.appContext.registerReceiver(powerReceiver, IntentFilter("android.intent.action.ACTION_POWER_DISCONNECTED"))
            Log.d(tag, "Detector is enabled")
        }
    }

    override fun isEnable(): Boolean {
        return enabled
    }
}