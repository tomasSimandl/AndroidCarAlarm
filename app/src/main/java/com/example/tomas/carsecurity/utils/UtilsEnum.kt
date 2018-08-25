package com.example.tomas.carsecurity.utils

import com.example.tomas.carsecurity.context.MyContext

enum class UtilsEnum {
    Alarm, Tracker;

    fun getInstance(context: MyContext, utilsHelper: UtilsHelper): GeneralUtil{
        return when (this){
            Alarm -> Alarm(context, utilsHelper)
            Tracker -> Tracker(context, utilsHelper)
        }
    }
}