package com.example.tomas.carsecurity.utils

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjString
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.communication.SmsProvider
import com.example.tomas.carsecurity.context.AlarmContext
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.sensors.MoveDetector
import com.example.tomas.carsecurity.sensors.SoundDetector
import java.util.*
import com.example.tomas.carsecurity.ObservableEnum as OEnum

class Alarm(context: MyContext, private val utilsHelper: UtilsHelper) : GeneralUtil(utilsHelper) {

    private val alarmContext = AlarmContext(context.sharedPreferences, context.appContext)

    private val tag = "utils.Alarm"

    private var isEnabled = false
    private var isAlarm = false
    private var isAlert = false

    private var systemEnabledTime = -1L
    private var lastLocation: Location? = null
    private var timer: Timer? = null

    override val thisUtilEnum: UtilsEnum = UtilsEnum.Alarm


    companion object Check: CheckObjString {
        override fun check(context: Context, sharedPreferences: SharedPreferences): String {

            val smsCheck = SmsProvider.check(context, sharedPreferences)

            return when (smsCheck) {  // TODO use strings from resources
                CheckCodes.hardwareNotSupported -> "Alarm needs to send SMS messages to warn car owner but their are not supported by this device."
                CheckCodes.permissionDenied -> "Alarm needs to send SMS messages to warn car owner but application is not permitted to send SMS messages."
                CheckCodes.notAllowed -> "Alarm needs to send SMS messages to warn car owner but their are disabled by user."
                else -> {
                    val moveCheck = MoveDetector.check(context, sharedPreferences)
                    val soundCheck = SoundDetector.check(context, sharedPreferences)

                    if (moveCheck == CheckCodes.success || soundCheck == CheckCodes.success) {
                        ""
                    } else {
                        "Alarm needs at least one detection sensor. No sensor is available.\nMoves sensor:\n" + CheckCodes.toString(moveCheck) + "\nMicrophone:\n" + CheckCodes.toString(soundCheck)
                    }
                }
            }
        }
    }


    override fun action(observable: Observable, args: Any?) {
        if (!isEnabled) return

        when (observable) {
            is LocationProvider -> onLocationUpdate(args as Location)
            is GeneralObservable -> onSensorUpdate(observable)
        }
    }

    private fun onLocationUpdate(location: Location) {
        Log.d(tag, """Location update: $location""")

        this.lastLocation = location
        utilsHelper.communicationManager.sendLocation(location, true)
    }

    private fun onSensorUpdate(observable: GeneralObservable) {

        val currentTime = Calendar.getInstance().timeInMillis
        Log.d(tag, """Alarm: detection by $observable at $currentTime.""")

        // alarm is already activated -> no work
        if (isAlarm) {
            Log.d(tag, "Alarm is already activated.")
            return
        }

        // detections are ignored because start alarm interval did not passed.
        if (currentTime - systemEnabledTime < alarmContext.startAlarmInterval) {
            Log.d(tag, "Alarm is waiting for activation")
            return
        }

        // first detection alarm is switched to alert mode
        if (!isAlert) {
            Log.d(tag, "Alarm alert mode activated.")
            isAlert = true

            val timerTask = object : TimerTask() {
                override fun run() {

                    val task = Runnable {
                        isAlarm = true
                        onAlarm()
                    }
                    utilsHelper.runOnUtilThread(task) // runOnUtilThread because timer run in own thread.
                }
            }
            timer = Timer("TimerThread")
            timer!!.schedule(timerTask, alarmContext.alertAlarmInterval.toLong())
        }
    }

    private fun onAlarm() {
        Log.d(tag, "Alarm was activated.")

        utilsHelper.communicationManager.sendAlarm()
        // TODO notify observers (Siren, ...) WARNING - notifyObservers is used in enable/disable
        // TODO send messages
        // TODO get actual location
        // TODO send actual location in loop

        utilsHelper.registerObserver(OEnum.LocationProvider, this) // TODO dynamically register and unregister to save battery.

//        while(isAlarm){
//            if(lastLocation != null){
//                utilsHelper.registerObserver(OEnum.LocationProvider, this)
//            }
//        }
    }

    override fun enable() {
        if (!isEnabled && canRun()) {

            isEnabled = true
            isAlarm = false
            isAlert = false
            systemEnabledTime = Calendar.getInstance().timeInMillis

            utilsHelper.registerObserver(OEnum.MoveDetector, this)
            utilsHelper.registerObserver(OEnum.SoundDetector, this)

            setChanged()
            notifyObservers(true)

            Log.d(tag, "Alarm system is enabled")
        }

        utilsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, true)
    }

    override fun disable() {
        if (isEnabled) {

            utilsHelper.unregisterAllObservables(this)
            timer?.cancel()
            isEnabled = false
            lastLocation = null
            systemEnabledTime = -1L

            setChanged()
            notifyObservers(false)

            Log.d(tag, "Alarm system disabled")
        }

        utilsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, false)
    }

    override fun isEnabled(): Boolean {
        return isEnabled
    }

    private fun canRun(): Boolean {
        val msg = check(alarmContext.context, alarmContext.sharedPreferences)

        return if (msg.isBlank()) {
            true
        } else {
            setChanged()
            notifyObservers(msg)
            false
        }
    }
}