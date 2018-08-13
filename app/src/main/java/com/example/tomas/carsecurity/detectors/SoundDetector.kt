package com.example.tomas.carsecurity.detectors

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.support.v4.content.ContextCompat
import com.example.tomas.carsecurity.R
import java.io.IOException

/**
 * This class is used for sound detecting. When sound is detected only calls parents
 * methods [setChanged] and [notifyObservers].
 *
 * @property context which is used for getting sharedPreferences, resources and checking permissions.
 */
class SoundDetector(private val context : Context) : GeneralDetector() {

    /** Contains private shared preferences which are shared across application. */
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE)

    /** Contains default value from resources for maximal allowed amplitude before alarm is triggered. */
    private val defMaxAmplitude = context.resources.getInteger(R.integer.default_max_ok_amplitude)
    /** Contains default value from resources for measure interval. */
    private val defMeasureInterval = context.resources.getInteger(R.integer.default_sound_detector_interval)

    /** Return maximal allowed amplitude before alarm is triggered. Value is from shared preferences or it is default value. */
    private val maxAmplitude
    get() = sharedPreferences.getInt(context.getString(R.string.key_max_ok_amplitude), defMaxAmplitude)

    /** Return measure interval from shared preferences or use default value */
    private val measureInterval
    get() = sharedPreferences.getLong(context.getString(R.string.key_sound_detector_interval), defMeasureInterval.toLong())

    /** Class used for audio recording. When sound detector is disable variable should be null */
    private var recorder: MediaRecorder? = null
    /** Indicates when sound detector is enabled. */
    private var enabled = false


    /**
     * Method stop sound detector and stop recording audio from microphone.
     */
    override fun disable() {
        enabled = false
        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    /**
     * Method starts recording audio from microphone but audio is not saved. At the end is call
     * method [initSoundChecker]
     *
     * It is possible that recording will not start.
     */
    override fun enable() {
        if(enabled || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
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
            return
        }
        recorder?.start()

        initSoundChecker()
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
     * is over limit which is set by [maxAmplitude] variable. Interval of controlling last maximal
     * amplitude is set by variable [measureInterval].
     *
     * When sound detector is disabled thread ends before next amplitude checking. Can be in sleep
     * state for maximal [measureInterval] milliseconds before it ends.
     */
    private fun initSoundChecker(){
        Thread {
            while(enabled) {
                val amplitude = recorder?.maxAmplitude ?: 0
                if (amplitude > maxAmplitude) {
                    println("""Max amplitude $amplitude is over limit $maxAmplitude""") // TODO log dedug

                    setChanged()
                    notifyObservers()
                }
                Thread.sleep(measureInterval)
            }
        }.start()
    }
}