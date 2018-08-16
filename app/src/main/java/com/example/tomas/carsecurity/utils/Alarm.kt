package com.example.tomas.carsecurity.utils

import android.location.Location
import android.widget.TextView
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.sensors.MoveDetector
import com.example.tomas.carsecurity.sensors.SoundDetector
import java.util.*

class Alarm(private val context: MyContext, private val textView: TextView) : IGeneralUtil{ // TODO remove text view


    override fun update(observable: Observable?, args: Any?) {

        when(observable){
            is GeneralObservable -> onAlarm(observable)
            is LocationProvider -> onLocationUpdate(args as Location)
        }
    }

    private fun printLocation(location: Location){
        textView.text = ""
        textView.append("""lat: ${location.latitude}, lon: ${location.longitude}""")
        textView.append("\n")
        textView.append("""alt: ${location.altitude}, acurrency: ${location.accuracy}""")
        textView.append("\n")
        textView.append("""speed: ${location.speed}""")
        textView.append("\n")
        textView.append("""provider: ${location.provider}""")
    }

    private fun onLocationUpdate(location: Location){
        printLocation(location)
    }


    private fun onAlarm(observable: GeneralObservable){
        when(observable){
            is SoundDetector -> println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! SOUND ALARM !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            is MoveDetector ->  println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! MOVE  ALARM !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            else ->             println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! UNDEF ALARM !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        }
    }

    fun enableAlarm(){

    }

    fun disableArarm(){

    }
}