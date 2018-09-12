package com.example.tomas.carsecurity.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.context.MyContext

class BatteryDetector (private val context: MyContext): GeneralObservable() {

    private val tag = "BatteryDetector"

    private var enabled = false

    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, """Battery action: ${intent.action}""")

            setChanged()
            notifyObservers(intent.action)
        }
    }


    override fun disable() {
        if (enabled) {
            enabled = false
            context.appContext.unregisterReceiver(batteryReceiver)
            Log.d(tag, "Detector is disabled")
        }
    }

    override fun enable() {
        if (!enabled) {
            enabled = true

            context.appContext.registerReceiver(batteryReceiver, IntentFilter("android.intent.action.BATTERY_LOW"))
            context.appContext.registerReceiver(batteryReceiver, IntentFilter("android.intent.action.BATTERY_OKAY"))
            context.appContext.registerReceiver(batteryReceiver, IntentFilter("android.intent.action.ACTION_POWER_CONNECTED"))
            context.appContext.registerReceiver(batteryReceiver, IntentFilter("android.intent.action.ACTION_POWER_DISCONNECTED"))
            Log.d(tag, "Detector is enabled")
        }
    }

    override fun isEnable(): Boolean {
        return enabled
    }
}