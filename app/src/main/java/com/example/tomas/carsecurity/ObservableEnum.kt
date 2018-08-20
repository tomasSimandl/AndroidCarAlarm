package com.example.tomas.carsecurity

import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.UtilsManagerContext

enum class ObservableEnum {
    MoveDetector, SoundDetector, LocationProvider;

    fun getInstance(context: MyContext): GeneralObservable {

        return when (this){
            MoveDetector -> com.example.tomas.carsecurity.sensors.MoveDetector(context)
            SoundDetector -> com.example.tomas.carsecurity.sensors.SoundDetector(context)
            LocationProvider -> com.example.tomas.carsecurity.sensors.LocationProvider(context)
        }
    }

    fun isAvailable(context: UtilsManagerContext): Boolean {
        return when (this){
            MoveDetector -> context.isMoveDetector
            SoundDetector -> context.isSoundDetector
            LocationProvider -> context.isLocationProvider
        }
    }
}