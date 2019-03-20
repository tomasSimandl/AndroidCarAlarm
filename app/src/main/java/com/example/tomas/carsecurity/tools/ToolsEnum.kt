package com.example.tomas.carsecurity.tools

import com.example.tomas.carsecurity.context.MyContext

enum class ToolsEnum {
    Alarm, Tracker, Battery;

    fun getInstance(context: MyContext, toolsHelper: ToolsHelper): GeneralTool{
        return when (this){
            Alarm -> Alarm(context, toolsHelper)
            Tracker -> Tracker(context, toolsHelper)
            Battery -> BatteryManager(context, toolsHelper)
        }
    }
}