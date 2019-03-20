package com.example.tomas.carsecurity.tools

import android.content.SharedPreferences
import android.util.Log
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.MessageType
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.BatteryDetector
import java.util.*

class BatteryManager (private val context: MyContext, private val utilsHelper: UtilsHelper): GeneralUtil(utilsHelper), SharedPreferences.OnSharedPreferenceChangeListener {

    override val thisUtilEnum: UtilsEnum = UtilsEnum.Battery
    private val tag = "BatteryManager"
    private var enabled = false
    private var shouldBeSaveMode = false

    override fun canEnable(): Boolean {
        return true
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        val task = Runnable {
            when (key) {
                context.appContext.getString(R.string.key_tool_battery_mode_is_allowed) -> changePowerSaveMode()
            }
        }
        utilsHelper.runOnUtilThread(task)
    }

    override fun action(observable: Observable, args: Any?) {
        if (observable is BatteryDetector) {
            val triple = args as Triple<*, *, *>

            val percent = (triple.third as Float * 100).toInt()

            when(triple.first){
                "android.intent.action.BATTERY_CHANGED" -> batteryChanged(percent, triple.second as Boolean)
                "android.intent.action.ACTION_POWER_CONNECTED" -> batteryConnected(percent, triple.second as Boolean)
                "android.intent.action.ACTION_POWER_DISCONNECTED" -> batteryDisconnected(percent, triple.second as Boolean)
            }
        }
    }


    private fun batteryChanged(percent: Int, charging: Boolean) {
        Log.d(tag, """Battery status changed. Capacity: $percent Charging: $charging""")

        if (percent <= context.utilsContext.batteryCriticalLevel) {
            if (!shouldBeSaveMode) {
                utilsHelper.communicationManager.sendEvent(MessageType.BatteryWarn, percent.toString(), "% of battery")
                shouldBeSaveMode = true
                changePowerSaveMode()
            }
        } else {
            if (shouldBeSaveMode) {
                shouldBeSaveMode = false
                changePowerSaveMode()
            }
        }
    }

    private fun changePowerSaveMode(){
        if(context.utilsContext.isBatteryModeAllowed && shouldBeSaveMode) {
            context.utilsContext.enablePowerSaveMode()
        } else {
            context.utilsContext.disablePowerSaveMode()
        }
    }

    private fun batteryConnected(percent: Int, charging: Boolean) {
        Log.d(tag, """Battery power is connected. Capacity: $percent Charging: $charging""")
        utilsHelper.communicationManager.sendEvent(MessageType.PowerConnected, percent.toString(), "% of battery")
    }

    private fun batteryDisconnected(percent: Int, charging: Boolean) {
        Log.d(tag, """Battery power is disconnected. Capacity: $percent Charging: $charging""")
        utilsHelper.communicationManager.sendEvent(MessageType.PowerDisconnected, percent.toString(), "% of battery")
    }


    override fun enable() {
        if (!enabled) {

            enabled = true
            utilsHelper.registerObserver(ObservableEnum.BatteryDetector, this)
            context.utilsContext.registerOnPreferenceChanged(this)
        }
    }

    override fun disable(force: Boolean) {
        if (force && enabled) {

            enabled = false
            context.utilsContext.unregisterOnPreferenceChanged(this)
            utilsHelper.unregisterAllObservables(this)
        }
    }

    override fun isEnabled(): Boolean {
        return enabled
    }
}