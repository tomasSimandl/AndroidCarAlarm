package com.example.tomas.carsecurity.tools

import android.content.SharedPreferences
import android.util.Log
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.MessageType
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.BatteryDetector
import com.example.tomas.carsecurity.utils.BatteryUtil
import java.util.*

/**
 * Class is used for control battery status and inform user about changes.
 *
 * @param context is my context used mainly for access of shared preferences values.
 * @param toolsHelper used mainly for registration of sensors
 */
class BatteryManager(private val context: MyContext, private val toolsHelper: ToolsHelper) :
        GeneralTool(toolsHelper),
        SharedPreferences.OnSharedPreferenceChangeListener {

    /** Logger tag */
    private val tag = "BatteryManager"

    /** Identification of this tool by [ToolsEnum] */
    override val thisToolEnum: ToolsEnum = ToolsEnum.Battery

    /** Identification if [BatteryManager] is enabled. */
    private var enabled = false

    /**
     * Tool does not need any prerequisites so this method return true.
     *
     * @return true
     */
    override fun canEnable(): Boolean {
        return true
    }

    /**
     * Method is automatically trigger when any value in shared preferences is changed. Method
     * only take action when values with key tool_battery_mode_is_allowed or
     * tool_battery_critical_level is changed.
     *
     * @param p0 is sharedPreferences storage in which was value changed.
     * @param key is key ov preference which was changed.
     */
    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        val task = Runnable {
            when (key) {
                context.appContext.getString(R.string.key_tool_battery_critical_level),
                context.appContext.getString(R.string.key_tool_battery_mode_is_allowed) -> {
                    val pair = BatteryUtil.getBatteryStatus(context.appContext)
                    val percent = (pair.first * 100).toInt()
                    if(pair.first != -1f) batteryChanged(percent, pair.second)
                }
            }
        }
        toolsHelper.runOnUtilThread(task)
    }

    /**
     * This method is called when any observed sensor calls [notifyObservers] method. Only
     * acceptable [Observable] object is [BatteryDetector].
     *
     * @param observable is [Observable] object which call [notifyObservers] method.
     * @param args are additional arguments which was transfer with observation. Expected is
     *              triple [action name; is charging; battery level <0,1>]
     */
    override fun action(observable: Observable, args: Any?) {
        if (observable is BatteryDetector) {
            val triple = args as Triple<*, *, *>

            val percent = (triple.third as Float * 100).toInt()

            when (triple.first) {
                "android.intent.action.BATTERY_CHANGED" -> batteryChanged(percent, triple.second as Boolean)
                "android.intent.action.ACTION_POWER_CONNECTED" -> batteryConnected(percent, triple.second as Boolean)
                "android.intent.action.ACTION_POWER_DISCONNECTED" -> batteryDisconnected(percent, triple.second as Boolean)
            }
        }
    }


    /**
     * Method decide if save mode should be activated or deactivated based on input params and
     * variable and values from shared preferences.
     *
     * @param percent level of battery <0,100>
     * @param charging indication if device is connected to external source of energy
     */
    private fun batteryChanged(percent: Int, charging: Boolean) {
        Log.d(tag, """Battery status changed. Capacity: $percent Charging: $charging""")

        if (context.toolsContext.isBatteryModeAllowed){
            // is allowed
            if (percent <= context.toolsContext.batteryCriticalLevel) {
                // should be activated
                if (!context.toolsContext.isPowerSaveMode) {
                    context.toolsContext.enablePowerSaveMode()
                    toolsHelper.communicationManager.sendEvent(MessageType.BatteryWarn, percent.toString(), "% of battery")
                }

                return
            }
        }

        if (context.toolsContext.isPowerSaveMode) {
            // should be deactivated
            context.toolsContext.disablePowerSaveMode()
        }
    }

    /**
     * Method send event to communication providers that device was connected to
     * external source of energy.
     *
     * @param percent level of battery <0,100>
     * @param charging indication if device is connected to external source of energy
     */
    private fun batteryConnected(percent: Int, charging: Boolean) {
        Log.d(tag, """Battery power is connected. Capacity: $percent Charging: $charging""")
        toolsHelper.communicationManager.sendEvent(MessageType.PowerConnected, percent.toString(), "% of battery")
    }

    /**
     * Method send event to communication providers that device was disconnected from external
     * source of energy.
     *
     * @param percent level of battery <0,100>
     * @param charging indication if device is connected to external source of energy
     */
    private fun batteryDisconnected(percent: Int, charging: Boolean) {
        Log.d(tag, """Battery power is disconnected. Capacity: $percent Charging: $charging""")
        toolsHelper.communicationManager.sendEvent(MessageType.PowerDisconnected, percent.toString(), "% of battery")
    }

    /**
     * Method enable [BatteryManager] and register all required sensors.
     * Method can be called repeatedly.
     */
    override fun enable() {
        if (!enabled) {

            enabled = true
            toolsHelper.registerObserver(ObservableEnum.BatteryDetector, this)
            context.toolsContext.registerOnPreferenceChanged(this)

            onSharedPreferenceChanged(null, context.appContext.getString(R.string.key_tool_battery_critical_level))
        }
    }

    /**
     * Method disable [BatteryManager] and unregister all required sensors. Disable only when
     * parameter [force] is true.
     * Method can be called repeatedly.
     *
     * @param force indication that [BatteryManager] must be disabled.
     */
    override fun disable(force: Boolean) {
        if (force && enabled) {

            enabled = false
            context.toolsContext.unregisterOnPreferenceChanged(this)
            toolsHelper.unregisterAllObservables(this)
        }
    }

    /**
     * Return if [BatteryManager] is enabled.
     * @return true when [BatteryManager] is enabled, false otherwise.
     */
    override fun isEnabled(): Boolean {
        return enabled
    }
}