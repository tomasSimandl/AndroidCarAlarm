package com.example.tomas.carsecurity.sensors

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.support.v4.content.ContextCompat
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjByte
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.SensorContext
import com.example.tomas.carsecurity.context.ToolsContext
import java.io.IOException
import java.util.*

/**
 * This class is used for sound detecting. When sound is detected only calls parents
 * methods [setChanged] and [notifyObservers].
 *
 * @property  context which is used for getting global data and preferences.
 */
class SoundDetector(private val context: MyContext) : GeneralObservable(), SharedPreferences.OnSharedPreferenceChangeListener {

    /** Logger tag */
    private val tag = "sensors.SoundDetector"

    /** Class used for audio recording. When sound detector is disable variable should be null */
    private var recorder: MediaRecorder? = null
    /** Indicates when sound detector is enabled. */
    private var enabled = false

    /** Timer which handles getting value from sound sensor with specific period. */
    private var timer: Timer? = null

    /**
     * Object is used for static access to check method.
     */
    companion object Check : CheckObjByte {
        /**
         * Method check is used detection if observation of sound sensor can be enabled.
         * @param context is application context
         * @return true when observation of sound sensor can be enabled.
         */
        override fun check(context: Context): Byte {
            return if(ToolsContext(context).isPowerSaveMode) {
                CheckCodes.notAllowedPowerSaveMode
            } else if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                CheckCodes.hardwareNotSupported
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                CheckCodes.permissionDenied
            } else if (!SensorContext(context).isSoundAllowed) {
                CheckCodes.notAllowed
            } else {
                CheckCodes.success
            }
        }
    }

    /**
     * Method indicates if observation of sound sensor can be enabled.
     *
     * @return true when observation of sound sensor by this method can be enabled.
     */
    override fun canEnable(): Boolean {
        return check(context.appContext) == CheckCodes.success
    }

    /**
     * Method is called when any value in shared preferences is changed. Method detects only change in sound measure
     * interval value. When this value is changed. Sound sensor is reinitialized.
     */
    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        val task = Runnable {
            when (key) {
                context.appContext.getString(R.string.key_sensor_sound_interval) ->
                    if (enabled && timer != null) {
                        timer?.cancel()
                        initSoundChecker()
                    }
            }
        }
        (context.mainServiceThreadLooper.thread as WorkerThread).postTask(task)
    }

    /**
     * Method stop sound detector and stop recording audio from microphone.
     */
    override fun disable() {
        if (enabled) {
            enabled = false
            timer?.cancel()
            timer = null
            context.sensorContext.unregisterOnPreferenceChanged(this)
            recorder?.stop()
            recorder?.release()
            recorder = null
        }
        Log.d(tag, "Detector is disabled")
    }

    /**
     * Method starts recording audio from microphone but audio is not saved. At the end is call
     * method [initSoundChecker]
     *
     * It is possible that recording will not start.
     */
    override fun enable() {
        if (!enabled && check(context.appContext) == CheckCodes.success) {

            enabled = true

            recorder = MediaRecorder()
            recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder?.setOutputFile("/dev/null")
            try {
                recorder?.prepare()
            } catch (e: IOException) {
                recorder = null
                enabled = false
                Log.w(tag, "Detector can to be enabled.")
                return
            }
            recorder?.start()

            initSoundChecker()
            context.sensorContext.registerOnPreferenceChanged(this)
        }
        Log.d(tag, "Detector is enabled.")
    }

    /**
     * Method returns if sound detector is enabled and if listening
     *
     * @return true if sound detector is enabled, false otherwise
     */
    override fun isEnable(): Boolean {
        return enabled
    }

    /**
     * Create separate thread which controlling amplitude of signal from microphone. If amplitude
     * is over limit which is set by [SensorContext].maxAmplitude variable.
     * Interval of controlling last maximal amplitude is set by variable
     * [SensorContext].measureInterval.
     *
     * When sound detector is disabled thread ends before next amplitude checking. Can be in sleep
     * state for maximal [SensorContext].measureInterval milliseconds before it ends.
     */
    private fun initSoundChecker() {
        val timerTask = object : TimerTask() {
            override fun run() {

                Log.v(tag, "timer thread was triggered.")

                val amplitude = recorder?.maxAmplitude ?: 0
                if (amplitude > context.sensorContext.maxAmplitude) {
                    Log.d(tag, """Max amplitude $amplitude is over limit ${context.sensorContext.maxAmplitude}""")

                    setChanged()
                    notifyObservers()
                    Log.d(tag, """Update - Thread: ${Thread.currentThread().name}""")
                }
            }
        }

        timer = Timer("SoundDetectorThread")
        timer?.schedule(timerTask, context.sensorContext.measureInterval.toLong(), context.sensorContext.measureInterval.toLong())
    }
}