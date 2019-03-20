package com.example.tomas.carsecurity.tools

import com.example.tomas.carsecurity.context.MyContext

enum class ToolsEnum {
    Alarm, Tracker, Battery;

    fun getInstance(context: MyContext, utilsHelper: UtilsHelper): GeneralTool{
        return when (this){
            Alarm -> Alarm(context, utilsHelper)
            Tracker -> Tracker(context, utilsHelper)
            Battery -> BatteryManager(context, utilsHelper)
        }
    }
}