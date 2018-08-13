package com.example.tomas.carsecurity.detectors

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.TypedValue
import com.example.tomas.carsecurity.R

/**
 * This class is used for movement detection. When movement is detected class call  only parents
 * methods [setChanged] and [notifyObservers].
 *
 * @property context which is used for getting sharedPreferences, resources and sensors.
 */
class MoveDetector(private val context: Context) : GeneralDetector(), SensorEventListener {

    /** Contains private shared preferences which are shared across application. */
    private val sharedPreferences: SharedPreferences

    /** Contains default value for accelerometer sensitivity which is taken from resources. */
    private val defSensitivity :Float
    /** Returns accelerometer sensitivity. Value is taken from shared preferences or it is used default value. */
    private val sensitivity
        get() = sharedPreferences.getFloat(context.getString(R.string.key_move_sensor_sensitivity), defSensitivity)

    /** Contains default dimension of data from accelerometer sensor. Value is taken from resources. */
    private val dimensions :Int

    /** Array contains data which are given by last acceleration sensor activity. */
    private var lastAcceleration :FloatArray
    /** Indicates if accelerometer return some data from last activation. */
    private var firstRun = false
    /** Indicates if move detector is enabled */
    private var enabled = false

    /** Class which is used for accelerometer sensor controling. */
    private val manager :SensorManager
    /** Class represents accelerometer sensor. */
    private val sensor :Sensor?


    /**
     * Constructor sets all uninitialized variables.
     */
    init {
        sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE)


        val outValue = TypedValue()
        context.resources.getValue(R.dimen.default_move_sensor_sensitivity, outValue, true)
        defSensitivity = outValue.float

        dimensions = context.resources.getInteger(R.integer.default_move_sensor_dimensions)
        lastAcceleration = FloatArray(dimensions)

        manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if(sensor == null){
            println("No acceleration sensor in this device") // TODO log error and return it to GUI
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
        }
    }

    /**
     * Method activate accelerometer sensor.
     */
    override fun enable() {
        if(!enabled) {
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            enabled = true
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
     * is greater than value of [sensitivity] variable.
     */
    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.values?.size == null || event.values.size != dimensions) {
            println("Incoming event is invalid or have invalid dimension") // TODO log warn
            return
        }

        if (firstRun) {
            lastAcceleration = event.values.copyOf()
            firstRun = false
            return
        }

        val euclideanDist = getEuclideanDistance(event.values, lastAcceleration)

        lastAcceleration = event.values.copyOf()

        if (euclideanDist > sensitivity) {
            setChanged()
            notifyObservers()
        }
    }

    /**
     * Calculates euclidean distance of two points given by two input arrays. Size of booth input
     * arrays must be equal to [dimensions] variable.
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

        for (i in 0 until dimensions) {
            val number = coordinatesA[i] - coordinatesB[i]
            sum += number * number
        }

        return Math.sqrt(sum).toFloat()
    }
}