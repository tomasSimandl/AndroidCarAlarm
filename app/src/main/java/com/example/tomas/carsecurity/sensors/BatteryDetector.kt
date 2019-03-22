package com.example.tomas.carsecurity.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.BatteryUtil

/**
 * Class is used for observation of battery. Any battery changes are propagate over Observer design pattern.
 *
 * @param context used for access values in shared preferences.
 */
class BatteryDetector(private val context: MyContext) : GeneralObservable() {

    /** Logger tag */
    private val tag = "BatteryDetector"

    /** Indications if observation of battery is enabled */
    private var enabled = false

    /**
     * Receiver which handles battery status changed actions.
     */
    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, """Battery action: ${intent.action}""")

            notifyObservers(intent.action, intent)
        }
    }

    /**
     * Receiver which handles Power connected and power disconnected actions.
     */
    private val powerReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, """Power action: ${intent.action}""")

            val batteryStatus = BatteryUtil.getBatteryStatus(context)

            if (batteryStatus.first != -1f) {
                setChanged()
                notifyObservers(Triple(intent.action, batteryStatus.second, batteryStatus.first))
            }
        }
    }

    /**
     * Method returns always true
     * @return true
     */
    override fun canEnable(): Boolean {
        return true
    }

    /**
     * Method notify observers of this class and sends them input action and actual battery state.
     *
     * @param action battery action which trigger this observation
     * @param intent which contains battery status information.
     */
    private fun notifyObservers(action: String?, intent: Intent) {

        if (action == null) return

        val isCharging: Boolean = BatteryUtil.batteryIsCharging(intent)
        val batteryPct: Float = BatteryUtil.batteryPct(intent)

        setChanged()
        notifyObservers(Triple(action, isCharging, batteryPct))
    }


    /**
     * Method disable observation of battery changes by unregistation from battery receivers.
     * Method can be called repeatedly.
     */
    override fun disable() {
        if (enabled) {
            enabled = false
            context.appContext.unregisterReceiver(batteryReceiver)
            context.appContext.unregisterReceiver(powerReceiver)
            Log.d(tag, "Detector is disabled")
        }
    }

    /**
     * Method enable observation of battery changes by registration of broadcast receivers.
     * Method can be called repeatedly.
     */
    override fun enable() {
        if (!enabled) {
            enabled = true

            context.appContext.registerReceiver(batteryReceiver, IntentFilter("android.intent.action.BATTERY_CHANGED"))
            context.appContext.registerReceiver(powerReceiver, IntentFilter("android.intent.action.ACTION_POWER_CONNECTED"))
            context.appContext.registerReceiver(powerReceiver, IntentFilter("android.intent.action.ACTION_POWER_DISCONNECTED"))
            Log.d(tag, "Detector is enabled")
        }
    }

    /**
     * Method indicates if battery observation is enabled.
     *
     * @return true if Battery observation is enabled, false otherwise
     */
    override fun isEnable(): Boolean {
        return enabled
    }
}