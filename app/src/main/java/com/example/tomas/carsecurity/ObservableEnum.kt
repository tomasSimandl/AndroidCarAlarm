package com.example.tomas.carsecurity

import android.util.Log
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.GeneralObservable

/**
 * Enum of all observable (sensors) used in application.
 */
enum class ObservableEnum {
    MoveDetector, SoundDetector, LocationProvider, BatteryDetector;

    /** Logger tag */
    private val tag = "ObservableEnum"

    /**
     * Create instance of observable given by this enum
     * @param context is my context used for initialization of observables.
     * @return created [GeneralObservable]
     */
    fun getInstance(context: MyContext): GeneralObservable {

        Log.d(tag, """Creating new instance of $this""")

        return when (this) {
            MoveDetector -> com.example.tomas.carsecurity.sensors.MoveDetector(context)
            SoundDetector -> com.example.tomas.carsecurity.sensors.SoundDetector(context)
            LocationProvider -> com.example.tomas.carsecurity.sensors.LocationProvider(context)
            BatteryDetector -> com.example.tomas.carsecurity.sensors.BatteryDetector(context)
        }
    }
}