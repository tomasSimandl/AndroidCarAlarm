package com.example.tomas.carsecurity

import android.content.Context
import android.util.Log
import com.example.tomas.carsecurity.context.MyContext

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

    fun isAvailable(context: Context): Boolean {
        return when (this){
            MoveDetector -> com.example.tomas.carsecurity.sensors.MoveDetector.check(context)
            SoundDetector -> com.example.tomas.carsecurity.sensors.SoundDetector.check(context)
            LocationProvider -> com.example.tomas.carsecurity.sensors.LocationProvider.check(context)
            BatteryDetector -> com.example.tomas.carsecurity.sensors.MoveDetector.check(context)

        } == CheckCodes.success
    }
}