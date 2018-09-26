package com.example.tomas.carsecurity.sensors

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjByte
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.SensorContext

/**
 * This class is used for movement detection. When movement is detected class call only parents
 * methods [setChanged] and [notifyObservers].
 *
 * @property context which is used for getting global data and preferences.
 */
class MoveDetector(private val context: MyContext) : GeneralObservable(), SensorEventListener {

    private val tag = "sensors.MoveDetector"

    /** Array contains data which are given by last acceleration sensor activity. */
    private var lastAcceleration :FloatArray
    /** Indicates if accelerometer return some data from last activation. */
    private var firstRun = false
    /** Indicates if move detector is enabled */
    private var enabled = false

    /** Class which is used for accelerometer sensor controlling. */
    private val manager :SensorManager
    /** Class represents accelerometer sensor. */
    private val sensor :Sensor?


    /**
     * Constructor sets all uninitialized variables.
     */
    init {
        lastAcceleration = FloatArray(context.sensorContext.dimensions)

        manager = context.appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if(sensor == null){
            Log.e(tag, "No acceleration sensor in this device")
        }
    }

    companion object Check: CheckObjByte {
        override fun check(context: Context): Byte {
            return if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
                CheckCodes.hardwareNotSupported
            } else if (!SensorContext(context).isMoveAllowed) {
                CheckCodes.notAllowed
            } else {
                CheckCodes.success
            }
        }
    }

    override fun canEnable(): Boolean {
        return check(context.appContext) == CheckCodes.success
    }

    /**
     * Method stop listening incoming data from accelerometer sensor. When now ones listening system
     * automatically turn off accelerometer sensor.
     */
    override fun disable() {
        if(enabled) {
            manager.unregisterListener(this)
            enabled = false
            firstRun = true
            Log.d(tag, "Detector is disabled")
        }
    }

    /**
     * Method activate accelerometer sensor.
     */
    override fun enable() {
        if(!enabled && sensor != null && check(context.appContext) == CheckCodes.success) {
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, Handler(context.mainServiceThreadLooper))
            enabled = true
            Log.d(tag, "Detector is enabled")
        }
    }

    /**
     * Method returns if move detector is enabled.
     *
     * @return true if move detector is enabled, false otherwise
     */
    override fun isEnable(): Boolean {
        return enabled
    }


    /**
     * Method is automatically triggered when accuracy of accelerometer was changed.
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // TODO do something
    }

    /**
     * Method is automatically triggered when accelerometer detect motion. When motion is significant
     * parent methods [setChanged] and [notifyObservers] are called.
     *
     * Motion is significant when euclidean distance between last sensor values and actual sensors
     * is greater than value of [SensorContext].sensitivity variable.
     */
    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.values?.size == null || event.values.size != context.sensorContext.dimensions) {
            Log.w(tag,"Incoming event is invalid or have invalid dimension")
            return
        }

        if (firstRun) {
            lastAcceleration = event.values.copyOf()
            firstRun = false
            return
        }

        val euclideanDist = getEuclideanDistance(event.values, lastAcceleration)

        lastAcceleration = event.values.copyOf()

        if (euclideanDist > context.sensorContext.sensitivity) {
            setChanged()
            notifyObservers()
            Log.d(tag, """Update - Thread: ${Thread.currentThread().name}""")
        }
    }

    /**
     * Calculates euclidean distance of two points given by two input arrays. Size of booth input
     * arrays must be equal to [SensorContext].dimensions variable.
     *
     * @param coordinatesA coordinates of first point
     * @param coordinatesB coordinates of second point
     * @return euclidean distance between two input points
     */
    private fun getEuclideanDistance(coordinatesA: FloatArray, coordinatesB: FloatArray): Float {
        //val euclideanDist: Double = Math.sqrt(
        // Math.pow((lastAcceleration[0] - event!!.values[0]).toDouble(), 2.0) +
        // Math.pow((lastAcceleration[1] - event.values[1]).toDouble(), 2.0) +
        // Math.pow((lastAcceleration[2] - event.values[2]).toDouble(), 2.0))

        var sum = 0.0

        for (i in 0 until context.sensorContext.dimensions) {
            val number = coordinatesA[i] - coordinatesB[i]
            sum += number * number
        }

        return Math.sqrt(sum).toFloat()
    }
}