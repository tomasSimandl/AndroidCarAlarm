package com.example.tomas.carsecurity.sensors

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.support.v4.content.ContextCompat
import android.util.Log
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.SoundDetectorContext
import java.io.IOException
import java.util.*

/**
 * This class is used for sound detecting. When sound is detected only calls parents
 * methods [setChanged] and [notifyObservers].
 *
 * @property  context which is used for getting global data and preferences.
 */
class SoundDetector(private val context : MyContext) : GeneralObservable() {

    private val tag = "sensors.SoundDetector"

    private val soundDetectorContext = SoundDetectorContext(context.sharedPreferences, context.appContext)

    /** Class used for audio recording. When sound detector is disable variable should be null */
    private var recorder: MediaRecorder? = null
    /** Indicates when sound detector is enabled. */
    private var enabled = false

    private var timer = Timer("SoundDetectorThread")


    /**
     * Method stop sound detector and stop recording audio from microphone.
     */
    override fun disable() {
        timer.cancel()
        enabled = false
        recorder?.stop()
        recorder?.release()
        recorder = null
        Log.d(tag, "Detector is disabled")
    }

    /**
     * Method starts recording audio from microphone but audio is not saved. At the end is call
     * method [initSoundChecker]
     *
     * It is possible that recording will not start.
     */
    override fun enable() {
        if(enabled || ContextCompat.checkSelfPermission(context.appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return

        enabled = true

        recorder = MediaRecorder()
        recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder?.setOutputFile("/dev/null")
        try {
            recorder?.prepare()
        } catch (e: IOException){
            recorder = null
            enabled = false // TODO maybe can throw exception
            Log.w(tag, "Detector can to be enabled.")
            return
        }
        recorder?.start()

        initSoundChecker()
        Log.d(tag, "Detector is enabled.")
    }

    /**
     * Method returns if sound detector is enabled and if listening
     *
     * @return true if sound detector is enabled, false otherwise
     */
    override fun isEnable() :Boolean {
        return enabled
    }

    /**
     * Create separate thread which controlling amplitude of signal from microphone. If amplitude
     * is over limit which is set by [soundDetectorContext].maxAmplitude variable.
     * Interval of controlling last maximal amplitude is set by variable
     * [soundDetectorContext].measureInterval.
     *
     * When sound detector is disabled thread ends before next amplitude checking. Can be in sleep
     * state for maximal [soundDetectorContext].measureInterval milliseconds before it ends.
     */
    private fun initSoundChecker(){

        val timerTask = object : TimerTask() {
            override fun run() {

                Log.v(tag, "timer thread was triggered.")

                val amplitude = recorder?.maxAmplitude ?: 0
                if (amplitude > soundDetectorContext.maxAmplitude) {
                    Log.d(tag,"""Max amplitude $amplitude is over limit ${soundDetectorContext.maxAmplitude}""")

                    setChanged()
                    notifyObservers()
                    Log.d(tag, """Update - Thread: ${Thread.currentThread().name}""")
                }
            }
        }
        timer = Timer("SoundDetectorThread")
        timer.schedule( timerTask, soundDetectorContext.measureInterval, soundDetectorContext.measureInterval)
    }
}