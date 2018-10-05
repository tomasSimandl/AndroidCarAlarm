package com.example.tomas.carsecurity.utils

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.media.MediaPlayer
import android.util.Log
import com.example.tomas.carsecurity.*
import com.example.tomas.carsecurity.communication.MessageType
import com.example.tomas.carsecurity.communication.SmsProvider
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.UtilsContext
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.sensors.MoveDetector
import com.example.tomas.carsecurity.sensors.SoundDetector
import java.util.*
import com.example.tomas.carsecurity.ObservableEnum as OEnum

class Alarm(private val context: MyContext, private val utilsHelper: UtilsHelper) : GeneralUtil(utilsHelper), SharedPreferences.OnSharedPreferenceChangeListener  {

    private val tag = "utils.Alarm"

    private var isEnabled = false
    private var isAlarm = false
    private var isAlert = false

    private var systemEnabledTime = -1L
    private var lastLocation: Location? = null
    private var timer: Timer? = null
    private var sendSmsTimer: Timer? = null
    private var mediaPlayer: MediaPlayer? = null

    override val thisUtilEnum: UtilsEnum = UtilsEnum.Alarm

    private val sendSmsTask = object: TimerTask() {
        override fun run() {
            if (lastLocation != null) {
                utilsHelper.communicationManager.sendLocation(lastLocation!!, true)
            }

            utilsHelper.registerObserver(OEnum.LocationProvider, this@Alarm) // on location update can unregister listener
        }
    }

    companion object Check: CheckObjString {
        override fun check(context: Context, skipAllow: Boolean): String {

            if(!skipAllow && !UtilsContext(context).isAlarmAllowed){
                return context.getString(R.string.error_alarm_disabled)
            }

            val smsCheck = SmsProvider.check(context)

            return when (smsCheck) {
                CheckCodes.hardwareNotSupported -> context.getString(R.string.error_alarm_sms_not_supported)
                CheckCodes.permissionDenied -> context.getString(R.string.error_alarm_sms_not_permitted)
                CheckCodes.notAllowed -> context.getString(R.string.error_alarm_sms_not_allowed)
                CheckCodes.invalidParameters -> context.getString(R.string.error_alarm_sms_invalid_params)
                else -> {
                    val moveCheck = MoveDetector.check(context)
                    val soundCheck = SoundDetector.check(context)

                    if (moveCheck == CheckCodes.success || soundCheck == CheckCodes.success) {
                        ""
                    } else {
                        context.getString(R.string.error_alarm_no_detector,
                                CheckCodes.toString(moveCheck, context),
                                CheckCodes.toString(soundCheck, context))
                    }
                }
            }
        }
    }

    override fun canEnable(): Boolean {
        return check(context.appContext, false).isBlank()
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        val task = Runnable {
            when (key) {
                context.appContext.getString(R.string.key_tool_battery_mode),
                context.appContext.getString(R.string.key_tool_alarm_send_location_interval) ->
                    if(isEnabled && isAlarm && sendSmsTimer != null){
                        sendSmsTimer?.cancel()
                        sendSmsTimer = Timer("SendSmsTimer")
                        sendSmsTimer!!.schedule(sendSmsTask, context.utilsContext.sendLocationInterval.toLong(), context.utilsContext.sendLocationInterval.toLong())
                    }
            }
        }
        utilsHelper.runOnUtilThread(task)
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
        if (context.utilsContext.sendLocationInterval > context.utilsContext.disableSendLocationInterval) {
            utilsHelper.unregisterObservable(OEnum.LocationProvider, this)
        }
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
        if (currentTime - systemEnabledTime < context.utilsContext.startAlarmInterval) {
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
            timer!!.schedule(timerTask, context.utilsContext.alertAlarmInterval.toLong())
        }
    }

    private fun onAlarm() {
        Log.d(tag, "Alarm was activated.")

        // send alarm to communication providers
        utilsHelper.communicationManager.sendAlarm()

        if(context.utilsContext.isCallAllow) {
            CallProvider(context).createCall()
        }

        // start siren
        if (context.utilsContext.isSirenAllow) {
            mediaPlayer = MediaPlayer.create(context.appContext, R.raw.car_alarm)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }


        // start send location loop
        if (context.communicationContext.isMessageAllowed(SmsProvider::class.java.name, MessageType.AlarmLocation.name, "send")) {
            utilsHelper.registerObserver(OEnum.LocationProvider, this)
            sendSmsTimer = Timer("SendSmsTimer")
            sendSmsTimer!!.schedule(sendSmsTask, context.utilsContext.sendLocationInterval.toLong(), context.utilsContext.sendLocationInterval.toLong())
        }
    }

    override fun enable() {
        assert(Thread.currentThread().name == "UtilsThread")
        if (!isEnabled && canRun()) {

            isEnabled = true
            isAlarm = false
            isAlert = false
            systemEnabledTime = Calendar.getInstance().timeInMillis
            sendSmsTimer = null
            timer = null

            utilsHelper.registerObserver(OEnum.MoveDetector, this)
            utilsHelper.registerObserver(OEnum.SoundDetector, this)

            setChanged()
            notifyObservers(true)

            Log.d(tag, "Alarm system is enabled")
            context.utilsContext.registerOnPreferenceChanged(this)
        }

        utilsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, true)
    }

    override fun disable(force: Boolean) {
        assert(Thread.currentThread().name == "UtilsThread")
        if (isEnabled) {

            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isEnabled = false

            utilsHelper.unregisterAllObservables(this)
            timer?.cancel()
            sendSmsTimer?.cancel()
            lastLocation = null
            systemEnabledTime = -1L

            context.utilsContext.unregisterOnPreferenceChanged(this)

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
        val msg = check(context.appContext, false)

        return if (msg.isBlank()) {
            true
        } else {
            setChanged()
            notifyObservers(msg)
            false
        }
    }
}