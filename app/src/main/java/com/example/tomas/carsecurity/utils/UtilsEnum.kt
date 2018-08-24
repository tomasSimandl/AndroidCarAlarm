package com.example.tomas.carsecurity.utils

import com.example.tomas.carsecurity.context.MyContext

enum class UtilsEnum {
    Alarm, Tracker;

    fun getInstance(context: MyContext, utilsManager: UtilsManager): GeneralUtil{
        return when (this){
            Alarm -> Alarm(context, utilsManager)
            Tracker -> Tracker(context,utilsManager)
        }
    }
}