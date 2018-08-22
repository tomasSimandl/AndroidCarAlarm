package com.example.tomas.carsecurity.utils

import android.location.Location
import android.util.Log
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.LocationProvider
import java.util.*
import com.example.tomas.carsecurity.ObservableEnum as OEnum

class Alarm(private val context: MyContext, private val utilsManager: UtilsManager) : GeneralUtil(context, utilsManager) {

    private val tag = "utils.Alarm"

    private var enabled = false
    private var alarm = false
    private var alert = false

    private var enabledTime = -1L
    private var lastLocation: Location? = null

    private val timer= Timer("TimerThread")

    override fun action(observable: Observable, args: Any?) {
        if(!enabled) return

        when(observable){
            is LocationProvider -> onLocationUpdate(args as Location)
            is GeneralObservable -> onDetection(observable)
        }
    }

    private fun onDetection(observable: GeneralObservable){

        val currentTime = Calendar.getInstance().timeInMillis

        Log.d(tag, """Alarm: detection by $observable at $currentTime.""")

        // alarm is already activated -> no work
        if(alarm) {
            Log.d(tag,"Alarm is already activated.")
            return
        }

        // detections are ignored because start alarm interval did not passed.
        if(currentTime - enabledTime < context.alarmContext.startAlarmInterval) {
            Log.d(tag,"Alarm is waiting for activation")
            return
        }

        // first detection alarm is switched to alert mode
        if(!alert) {
            Log.d(tag, "Alarm alert mode activated.")
            alert = true

            val timerTask = object : TimerTask() {
                override fun run() {

                    val task = Runnable {
                        alarm = true
                        onAlarm()
                    }
                    utilsManager.runOnUtilThread(task)
                }
            }

            timer.schedule( timerTask, context.alarmContext.alertAlarmInterval)
        }
    }

    private fun onAlarm(){
        Log.d(tag,"Alarm was activated.")
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
        Log.d(tag,"""Alarm get location: $location""")
    }

    override fun enable(){
        enabled = true
        alarm = false
        alert = false
        enabledTime = Calendar.getInstance().timeInMillis

        utilsManager.registerObserver(OEnum.MoveDetector, this)
        utilsManager.registerObserver(OEnum.SoundDetector, this)

        Log.d(tag,"Alarm system enabled")
        utilsManager.informUI(this, true)
    }

    override fun disable(){
        utilsManager.unregisterAllObservables(this)
        timer.cancel()
        enabled = false
        // TODO stop alarm operations

        Log.d(tag,"Alarm system disabled")
        utilsManager.informUI(this, false)
    }

    override fun isEnabled(): Boolean{
        return enabled
    }
}