package com.example.tomas.carsecurity

import android.util.Log
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.SensorContext

enum class ObservableEnum {
    MoveDetector, SoundDetector, LocationProvider, BatteryDetector;

    private val tag = "ObservableEnum"

    fun getInstance(context: MyContext): GeneralObservable {

        Log.d(tag, """Creating new instance of $this""")

        return when (this){
            MoveDetector -> com.example.tomas.carsecurity.sensors.MoveDetector(context)
            SoundDetector -> com.example.tomas.carsecurity.sensors.SoundDetector(context)
            LocationProvider -> com.example.tomas.carsecurity.sensors.LocationProvider(context)
            BatteryDetector -> com.example.tomas.carsecurity.sensors.BatteryDetector(context)
        }
    }

    fun isAvailable(context: SensorContext): Boolean {
        return when (this){
            MoveDetector -> context.isMoveAllowed
            SoundDetector -> context.isSoundAllowed
            LocationProvider -> context.isLocationAllowed
            BatteryDetector -> context.isBatteryAllowed
        }
    }
}