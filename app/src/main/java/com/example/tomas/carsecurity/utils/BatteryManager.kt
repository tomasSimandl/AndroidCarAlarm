package com.example.tomas.carsecurity.utils

import android.util.Log
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.BatteryDetector
import java.util.*

class BatteryManager (private val context: MyContext, private val utilsHelper: UtilsHelper): GeneralUtil(utilsHelper) {

    override val thisUtilEnum: UtilsEnum = UtilsEnum.Battery
    private val tag = "BatteryManager"
    private var enabled = false
    private var warnWasSend = false

    override fun canEnable(): Boolean {
        return true
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

        if (percent <= 15) {// TODO const
            utilsHelper.communicationManager.sendBatteryWarn(percent)
            if (!warnWasSend) {
                warnWasSend = true
                enablePowerSaveMode()
            }
        } else {
            if (warnWasSend) {
                warnWasSend = false
                disablePowerSaveMode()
            }
        }
    }

    private fun batteryConnected(percent: Int, charging: Boolean) {
        Log.d(tag, """Battery power is connected. Capacity: $percent Charging: $charging""")
        utilsHelper.communicationManager.sendPowerConnected(percent)
    }

    private fun batteryDisconnected(percent: Int, charging: Boolean) {
        Log.d(tag, """Battery power is disconnected. Capacity: $percent Charging: $charging""")
        utilsHelper.communicationManager.sendPowerDisconnected(percent)
    }

    private fun disablePowerSaveMode() {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun enablePowerSaveMode() {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun enable() {
        if (!enabled) {

            enabled = true
            utilsHelper.registerObserver(ObservableEnum.BatteryDetector, this)
        }
    }

    override fun disable(force: Boolean) {
        if (force && enabled) {

            enabled = false
            utilsHelper.unregisterAllObservables(this)
        }
    }

    override fun isEnabled(): Boolean {
        return enabled
    }
}