package com.example.tomas.carsecurity.tools

import com.example.tomas.carsecurity.context.MyContext

/**
 * Enum with names of existing tools.
 */
enum class ToolsEnum {
    Alarm, Tracker, Battery;

    /**
     * Method create and return instance of tool based on this enum.
     *
     * @param context is my context used mainly for access of shared preferences values.
     * @param toolsHelper used mainly for registration of sensors.
     * @return created instance of tool.
     */
    fun getInstance(context: MyContext, toolsHelper: ToolsHelper): GeneralTool {
        return when (this) {
            Alarm -> Alarm(context, toolsHelper)
            Tracker -> Tracker(context, toolsHelper)
            Battery -> BatteryManager(context, toolsHelper)
        }
    }
}