package com.example.tomas.carsecurity.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.context.MoveDetectorContext
import com.example.tomas.carsecurity.context.MyContext

/**
 * This class is used for movement detection. When movement is detected class call only parents
 * methods [setChanged] and [notifyObservers].
 *
 * @property context which is used for getting global data and preferences.
 */
class MoveDetector(private val context: MyContext) : GeneralObservable(), SensorEventListener {

    private val tag = "sensors.MoveDetector"

    private val moveDetectorContext = MoveDetectorContext(context.sharedPreferences, context.appContext)

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
        lastAcceleration = FloatArray(moveDetectorContext.dimensions)

        manager = context.appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if(sensor == null){
            Log.e(tag, "No acceleration sensor in this device") // TODO return error to GUI
        }
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
        if(!enabled) {
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
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
     * is greater than value of [moveDetectorContext.sensitivity] variable.
     */
    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.values?.size == null || event.values.size != moveDetectorContext.dimensions) {
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

        if (euclideanDist > moveDetectorContext.sensitivity) {
            setChanged()
            notifyObservers()
        }
    }

    /**
     * Calculates euclidean distance of two points given by two input arrays. Size of booth input
     * arrays must be equal to [moveDetectorContext.dimensions] variable.
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

        for (i in 0 until moveDetectorContext.dimensions) {
            val number = coordinatesA[i] - coordinatesB[i]
            sum += number * number
        }

        return Math.sqrt(sum).toFloat()
    }
}