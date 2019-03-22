package com.example.tomas.carsecurity.tools

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.media.MediaPlayer
import android.util.Log
import com.example.tomas.carsecurity.*
import com.example.tomas.carsecurity.communication.MessageType
import com.example.tomas.carsecurity.communication.sms.SmsProvider
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.ToolsContext
import com.example.tomas.carsecurity.sensors.GeneralObservable
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.sensors.MoveDetector
import com.example.tomas.carsecurity.sensors.SoundDetector
import com.example.tomas.carsecurity.utils.CallProvider
import com.example.tomas.carsecurity.storage.entity.Location as DBLocation
import java.util.*
import com.example.tomas.carsecurity.ObservableEnum as OEnum

class Alarm(private val context: MyContext, private val toolsHelper: ToolsHelper) : GeneralTool(toolsHelper), SharedPreferences.OnSharedPreferenceChangeListener  {

    private val tag = "tools.Alarm"

    private var isEnabled = false
    private var isAlarm = false
    private var isAlert = false

    private var systemEnabledTime = -1L
    private var lastLocation: Location? = null
    private var timer: Timer? = null
    private var sendSmsTimer: Timer? = null
    private var mediaPlayer: MediaPlayer? = null

    override val thisUtilEnum: ToolsEnum = ToolsEnum.Alarm

    companion object Check: CheckObjString {
        override fun check(context: Context, skipAllow: Boolean): String {

            if(!skipAllow && !ToolsContext(context).isAlarmAllowed){
                return context.getString(R.string.error_alarm_disabled)
            }

            val moveCheck = MoveDetector.check(context)
            val soundCheck = SoundDetector.check(context)

            return if (moveCheck == CheckCodes.success || soundCheck == CheckCodes.success) {
                ""
            } else {
                context.getString(R.string.error_alarm_no_detector,
                        CheckCodes.toString(moveCheck, context),
                        CheckCodes.toString(soundCheck, context))
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
                        scheduleSmsTimer()
                    }
            }
        }
        toolsHelper.runOnUtilThread(task)
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
        if (context.toolsContext.sendLocationInterval > context.toolsContext.disableSendLocationInterval) {
            toolsHelper.unregisterObservable(OEnum.LocationProvider, this)
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
        if (currentTime - systemEnabledTime < context.toolsContext.startAlarmInterval) {
            Log.d(tag, "Alarm is waiting for activation")
            return
        }

        // first detection alarm is switched to alert mode
        if (!isAlert) {
            Log.d(tag, "Alarm alert mode activated.")
            isAlert = true

            val timerTask = object : TimerTask() {
                override fun run() {
                    toolsHelper.runOnUtilThread( // runOnUtilThread because timer run in own thread.
                            Runnable {
                                isAlarm = true
                                onAlarm()
                            })
                }
            }
            timer = Timer("TimerThread")
            timer!!.schedule(timerTask, context.toolsContext.alertAlarmInterval.toLong())
        }
    }

    private fun onAlarm() {
        Log.d(tag, "Alarm was activated.")

        // send alarm to communication providers
        toolsHelper.communicationManager.sendEvent(MessageType.Alarm, Calendar.getInstance().time.toString())

        if(context.toolsContext.isCallAllow) {
            CallProvider(context).createCall()
        }

        // start siren
        if (context.toolsContext.isSirenAllow) {
            mediaPlayer = MediaPlayer.create(context.appContext, R.raw.car_alarm)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }


        // start send location loop
        if (context.communicationContext.isMessageAllowed(SmsProvider::class.java.name, MessageType.AlarmLocation.name, "send")) {
            toolsHelper.registerObserver(OEnum.LocationProvider, this)
            scheduleSmsTimer()
        }
    }

    private fun scheduleSmsTimer(){
        sendSmsTimer = Timer("SendSmsTimer")
        sendSmsTimer!!.schedule(getSmsTimerTask(), context.toolsContext.sendLocationInterval.toLong(), context.toolsContext.sendLocationInterval.toLong())
    }

    private fun getSmsTimerTask(): TimerTask {
        return object: TimerTask() {
            override fun run() {
                if (lastLocation != null) {
                    toolsHelper.communicationManager.sendLocation(DBLocation(lastLocation!!, null), true)
                }

                toolsHelper.registerObserver(OEnum.LocationProvider, this@Alarm) // on location update can unregister listener
            }
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

            toolsHelper.registerObserver(OEnum.MoveDetector, this)
            toolsHelper.registerObserver(OEnum.SoundDetector, this)

            setChanged()
            notifyObservers(true)

            Log.d(tag, "Alarm system is enabled")
            context.toolsContext.registerOnPreferenceChanged(this)

            toolsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, true)
        }

    }

    override fun disable(force: Boolean) {
        assert(Thread.currentThread().name == "UtilsThread")
        if (isEnabled) {

            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isEnabled = false

            toolsHelper.unregisterAllObservables(this)
            timer?.cancel()
            sendSmsTimer?.cancel()
            lastLocation = null
            systemEnabledTime = -1L

            context.toolsContext.unregisterOnPreferenceChanged(this)

            setChanged()
            notifyObservers(false)

            Log.d(tag, "Alarm system disabled")

            toolsHelper.communicationManager.sendUtilSwitch(thisUtilEnum, false)
        }

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