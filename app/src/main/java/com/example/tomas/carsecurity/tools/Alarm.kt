package com.example.tomas.carsecurity.tools

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.media.MediaPlayer
import android.util.Log
import com.example.tomas.carsecurity.*
import com.example.tomas.carsecurity.communication.MessageType
import com.example.tomas.carsecurity.communication.network.NetworkProvider
import com.example.tomas.carsecurity.communication.sms.SmsProvider
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.ToolsContext
import com.example.tomas.carsecurity.sensors.GeneralObservable
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.sensors.MoveDetector
import com.example.tomas.carsecurity.sensors.SoundDetector
import com.example.tomas.carsecurity.utils.CallProvider
import java.util.*
import com.example.tomas.carsecurity.ObservableEnum as OEnum
import com.example.tomas.carsecurity.storage.entity.Location as DBLocation

/**
 * Class represents alarm system and all its logic.
 *
 * @param context is my context used mainly for access of shared preferences values.
 * @param toolsHelper used mainly for registration of sensors
 */
class Alarm(private val context: MyContext, private val toolsHelper: ToolsHelper) :
        GeneralTool(toolsHelper),
        SharedPreferences.OnSharedPreferenceChangeListener {

    /** Logger tag */
    private val tag = "tools.Alarm"

    /** Identification of this tool by [ToolsEnum] */
    override val thisToolEnum: ToolsEnum = ToolsEnum.Alarm

    /** Indication if alarm system is activated */
    private var isEnabled = false
    /** Indication if system is in alarm mode. */
    var isAlarm = false
    /** Indication if system is in alert mode. Interval for alarm turn of when disruption was detected. */
    private var isAlert = false

    /** Time when alarm starts. Used for startup interval */
    private var systemEnabledTime = -1L
    /** Contains value of last known location, used for sending over communication providers. */
    private var lastLocation: Location? = null
    /** Timer used for interval before alarm when disruption was detected. */
    private var alarmTimer: Timer? = null
    /** Timer for sending location SMS in specific intervals. */
    private var sendSmsTimer: Timer? = null
    /** Media player used for sending alarm sound */
    private var mediaPlayer: MediaPlayer? = null
    /** Indications if location should be send on next location update */
    private var shouldSendLocation = false


    /**
     * Object used for static access to [check] method.
     */
    companion object Check : CheckObjString {
        /**
         * Method checks if there is some restriction which prevents of alarm activation.
         *
         * @param context is application context
         * @param skipAllow indicates if should be check alarm allow attribute set by user.
         * @return Error message when there is some problem or empty string when alarm can be enabled.
         */
        override fun check(context: Context, skipAllow: Boolean): String {

            if (!skipAllow && !ToolsContext(context).isAlarmAllowed) {
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

    /**
     * Method return if alarm can be enabled with use of [check] method.
     *
     * @return true when alarm can be enabled, false otherwise.
     */
    override fun canEnable(): Boolean {
        return check(context.appContext, false).isBlank()
    }

    /**
     * Method is automatically called when any value in sharedPreferences is changed. Method interact only when
     * key of changed values are:
     * tool_battery_mode
     * tool_alarm_send_location_interval
     * In both cases method reinitialize timer for sending location sms messages.
     *
     * @param p0 is shared preferences storage in which was value changed.
     * @param key of value which was changed.
     */
    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        val task = Runnable {
            when (key) {
                context.appContext.getString(R.string.key_tool_battery_mode),
                context.appContext.getString(R.string.key_tool_alarm_send_location_interval) ->
                    if (isEnabled && isAlarm && sendSmsTimer != null) {
                        sendSmsTimer?.cancel()
                        scheduleSmsTimer()
                    }
            }
        }
        toolsHelper.runOnUtilThread(task)
    }

    /**
     * This method is called when any observed sensor calls [notifyObservers] method. Only acceptable [Observable]
     * objects are subclasses of [GeneralObservable].
     *
     * @param observable is [Observable] object which call [notifyObservers] method.
     * @param args are additional arguments which was transfer with observation.
     */
    override fun action(observable: Observable, args: Any?) {
        if (!isEnabled) return

        when (observable) {
            is LocationProvider -> onLocationUpdate(args as Location)
            is GeneralObservable -> onSensorUpdate(observable)
        }
    }

    /**
     * Method save location to [lastLocation] variable. When sms location interval is too big. Method unregister
     * location sensor from this class.
     *
     * @param location is new location received from [LocationProvider]
     */
    private fun onLocationUpdate(location: Location) {
        Log.d(tag, """Location update: $location""")
        this.lastLocation = location

        if (context.toolsContext.sendLocationInterval > context.toolsContext.disableSendLocationInterval) {
            toolsHelper.unregisterObservable(OEnum.LocationProvider, this)
        }

        if (lastLocation != null && shouldSendLocation) {
            toolsHelper.communicationManager.sendLocation(DBLocation(lastLocation!!, null), true)
            lastLocation = null
            shouldSendLocation = false
        }
    }

    /**
     * Method process sensor indication. Observations are ignored when start up interval did not pass. First detection
     * switch system to alert mode which automatically switch to alarm mode after specific time period.
     *
     * @param observable is [GeneralObservable] which detects some suspicion data.
     */
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
                    toolsHelper.runOnUtilThread( // runOnUtilThread because alarmTimer run in own thread.
                            Runnable {
                                isAlarm = true
                                onAlarm()
                            })
                }
            }
            alarmTimer = Timer("TimerThread")
            alarmTimer!!.schedule(timerTask, context.toolsContext.alertAlarmInterval.toLong())
        }
    }

    /**
     * Method trigger all actions when system switch to alarm mode.
     * 1) send event to communication providers
     * 2) call to specific number if it is allowed
     * 3) start siren if it is allowed
     * 4) start sending location sms if it is allowed
     */
    private fun onAlarm() {
        Log.d(tag, "Alarm was activated.")

        // send alarm to communication providers
        toolsHelper.communicationManager.sendEvent(MessageType.Alarm, Calendar.getInstance().time.toString())

        // Notify UI that alarm is triggered
        setChanged()
        notifyObservers(MainService.Actions.ActionAlarm)

        if (context.toolsContext.isCallAllow) {
            CallProvider(context).createCall()
        }

        // start siren
        if (context.toolsContext.isSirenAllow) {
            mediaPlayer = MediaPlayer.create(context.appContext, R.raw.car_alarm)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }

        // start send location loop
        val isSmsLocationAllowed = context.communicationContext.isMessageAllowed(
                SmsProvider::class.java.name, MessageType.AlarmLocation.name, "send")
        val isNetworkLocationAllowed = context.communicationContext.isMessageAllowed(
                NetworkProvider::class.java.name, "Alarm_Position_send")

        if (isSmsLocationAllowed || isNetworkLocationAllowed) {
            toolsHelper.registerObserver(OEnum.LocationProvider, this)
            scheduleSmsTimer()
        }
    }

    /**
     * Method schedule timer of sending location messages in specific time interval. Time interval is get from
     * shared preferences.
     */
    private fun scheduleSmsTimer() {
        sendSmsTimer = Timer("SendSmsTimer")
        sendSmsTimer!!.schedule(
                getSmsTimerTask(),
                context.toolsContext.sendLocationInterval.toLong(),
                context.toolsContext.sendLocationInterval.toLong())
    }

    /**
     * Method create and return task for sending location messages over communication provider.
     * @return created task for sending location messages.
     */
    private fun getSmsTimerTask(): TimerTask {
        return object : TimerTask() {
            override fun run() {

                if (lastLocation != null) {
                    toolsHelper.communicationManager.sendLocation(DBLocation(lastLocation!!, null), true)
                    lastLocation = null
                    shouldSendLocation = false
                } else {
                    // on location update can unregister listener
                    toolsHelper.registerObserver(OEnum.LocationProvider, this@Alarm)
                    shouldSendLocation = true
                }
            }
        }
    }

    /**
     * Method initialize and enable whole alarm system and inform all observers about this change.
     * This method can be called repeatedly.
     */
    override fun enable() {
        assert(Thread.currentThread().name == "UtilsThread")
        if (!isEnabled && canRun()) {

            isEnabled = true
            isAlarm = false
            isAlert = false
            systemEnabledTime = Calendar.getInstance().timeInMillis
            sendSmsTimer = null
            alarmTimer = null
            shouldSendLocation = false
            lastLocation = null

            toolsHelper.registerObserver(OEnum.MoveDetector, this)
            toolsHelper.registerObserver(OEnum.SoundDetector, this)

            setChanged()
            notifyObservers(true)

            Log.d(tag, "Alarm system is enabled")
            context.toolsContext.registerOnPreferenceChanged(this)

            toolsHelper.communicationManager.sendUtilSwitch(thisToolEnum, true)
        }

    }

    /**
     * Method disable whole alarm system and inform observers about this change.
     * @param force parameter force is not used in this implementation.
     */
    override fun disable(force: Boolean) {
        assert(Thread.currentThread().name == "UtilsThread")
        if (isEnabled) {

            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isEnabled = false
            isAlarm = false

            toolsHelper.unregisterAllObservables(this)
            alarmTimer?.cancel()
            sendSmsTimer?.cancel()
            lastLocation = null
            shouldSendLocation = false
            systemEnabledTime = -1L

            context.toolsContext.unregisterOnPreferenceChanged(this)

            setChanged()
            notifyObservers(false)

            Log.d(tag, "Alarm system disabled")

            toolsHelper.communicationManager.sendUtilSwitch(thisToolEnum, false)
        }

    }

    /**
     * Indicates if this alarm system is activated (enabled)
     * @return if alarm system is enabled
     */
    override fun isEnabled(): Boolean {
        return isEnabled
    }

    /**
     * Method use [check] method to check if alarm can be activated.
     * When activation is not possible send error message to all observers of this class.
     *
     * @return true when alarm can be activated, false otherwise.
     */
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