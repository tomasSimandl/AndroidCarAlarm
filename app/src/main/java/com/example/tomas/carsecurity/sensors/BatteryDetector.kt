package com.example.tomas.carsecurity.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.BatteryUtil
import java.util.*

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

    /** Indication if battery is charging */
    private var isCharging = false
    /** State of battery level */
    private var batteryCapacity = 1f
    /** Timer for periodic checking of battery */
    private var checkStatusTimer: Timer? = null

    /**
     * Receiver which handles Power connected and power disconnected actions.
     */
    private val powerReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, """Power action: ${intent.action}""")

            notifyObservers(intent.action ?: "android.intent.action.BATTERY_CHANGED")
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
     * Method notify observers of this class and sends them input action and actual battery state only if new battery
     * state is different from the last one.
     *
     * @param action battery action which trigger this observation
     */
    private fun notifyObservers(action: String) {

        Log.i(tag, "Actual battery status: isCharging [$isCharging], level [$batteryCapacity]")

        val batteryStatus = BatteryUtil.getBatteryStatus(context.appContext)

        if (batteryStatus.first != -1f &&
                (batteryCapacity != batteryStatus.first || isCharging != batteryStatus.second)) {

            Log.d(tag, "Battery status was changed.")

            batteryCapacity = batteryStatus.first
            isCharging = batteryStatus.second

            setChanged()
            notifyObservers(Triple(action, isCharging, batteryCapacity))
        }
    }


    /**
     * Method disable observation of battery changes by unregistation from battery receivers.
     * Method can be called repeatedly.
     */
    override fun disable() {
        if (enabled) {
            enabled = false
            context.appContext.unregisterReceiver(powerReceiver)

            checkStatusTimer?.cancel()
            checkStatusTimer = null

            Log.d(tag, "Detector is disabled")
        }
    }

    /**
     * Method enable observation of battery changes by registration of broadcast receivers and starts of periodic
     * battery checking.
     * Method can be called repeatedly.
     */
    override fun enable() {
        if (!enabled) {
            enabled = true
            context.appContext.registerReceiver(
                    powerReceiver,
                    IntentFilter("android.intent.action.ACTION_POWER_CONNECTED"))
            context.appContext.registerReceiver(
                    powerReceiver,
                    IntentFilter("android.intent.action.ACTION_POWER_DISCONNECTED"))

            // battery last status initialization
            val batteryStatus = BatteryUtil.getBatteryStatus(context.appContext)
            batteryCapacity = batteryStatus.first
            isCharging = batteryStatus.second


            // getting scheduler check interval from properties
            val batteryCheckIntervalProperty = context.sensorContext.properties["battery.check.interval.millis"] as String?
            val batteryCheckInterval: Long = batteryCheckIntervalProperty?.toLongOrNull() ?: 300000L

            // schedule timer for periodic checking of battery status
            checkStatusTimer = Timer("Check battery timer")
            checkStatusTimer!!.schedule(checkStatusTask, batteryCheckInterval, batteryCheckInterval)
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

    /**
     * Task which only call method notifyObservers with action BATTERY_CHANGED.
     */
    private val checkStatusTask: TimerTask
        get() = object : TimerTask() {
            override fun run() {
                notifyObservers("android.intent.action.BATTERY_CHANGED")
            }
        }
}