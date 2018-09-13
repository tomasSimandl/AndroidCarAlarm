package com.example.tomas.carsecurity.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.context.MyContext

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

            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }

            if(batteryStatus != null) {
                notifyObservers(intent.action, batteryStatus)
            }
        }
    }

    private fun notifyObservers(action: String?, intent: Intent){

        if(action == null) return

        val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL


        val batteryPct: Float? = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) /
                intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1).toFloat()

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