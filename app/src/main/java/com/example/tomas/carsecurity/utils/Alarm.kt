package com.example.tomas.carsecurity.utils

import android.location.Location
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.MainActivity
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.LocationProvider
import java.util.*
import com.example.tomas.carsecurity.ObservableEnum as OEnum

class Alarm(private val context: MyContext, private val utilsManager: UtilsManager) : GeneralUtil(context, utilsManager) {

    private var enabled = false
    private var alarm = false
    private var alert = false

    private var lastDetection = -1L
    private var enableTime = -1L

    private var lastLocation: Location? = null

    override fun action(observable: Observable, args: Any?) {

        if(!enabled) return

        when(observable){
            is LocationProvider -> onLocationUpdate(args as Location)
            is GeneralObservable -> onDetection(observable)
        }
    }

    private fun onDetection(observable: GeneralObservable){

        val currentTime = Calendar.getInstance().timeInMillis

        println("""Alarm: detection by $observable at $currentTime.""") // TODO log

        // alarm is already activated -> no work
        if(alarm) return

        // detections are ignored because start alarm interval did not passed.
        if(currentTime - enableTime < context.alarmContext.startAlarmInterval) return

        // first detection alarm is switched to alert mode
        if(!alert) {
            lastDetection = currentTime
            alert = true
            return
        }

        val timeDiff = currentTime - lastDetection
        // time interval which eliminates short moves did not passed
        if(timeDiff < context.alarmContext.ignoreAlarmInterval) return

        // if separation is preparation for algorithm extension

        // time before is alarm triggered after detection did not passed
        if(timeDiff < context.alarmContext.alertAlarmInterval) return

        alarm = true
        onAlarm()
    }

    private fun onAlarm(){
        println("Alarm was activated.") // TODO log
        // TODO notify observers (Siren, ...)
        // TODO send messages
        // TODO get actual location
        // TODO send actual location in loop

        utilsManager.registerObserver(OEnum.LocationProvider, this) // TODO dynamically register and unregister to save battery.

//        while(true){
//            if(lastLocation != null){
//
//            }
//        }
    }


    private fun onLocationUpdate(location: Location){
        this.lastLocation = location
        println("""Alarm get location: $location""")
    }

    override fun enable(){
        enabled = true
        alarm = false
        alert = false
        enableTime = Calendar.getInstance().timeInMillis

        utilsManager.registerObserver(OEnum.MoveDetector, this)
        utilsManager.registerObserver(OEnum.SoundDetector, this)

        println("Alarm system enabled")
        utilsManager.informUI(this, true)
    }

    override fun disable(){
        utilsManager.unregisterAllObservables(this)

        enabled = false
        // TODO stop alarm operations

        println("Alarm system disabled")
        utilsManager.informUI(this, false)
    }

    override fun isEnabled(): Boolean{
        return enabled
    }
}