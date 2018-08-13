package com.example.tomas.carsecurity

import android.location.Location
import android.widget.TextView
import com.example.tomas.carsecurity.detectors.GeneralDetector
import com.example.tomas.carsecurity.detectors.MoveDetector
import com.example.tomas.carsecurity.detectors.SoundDetector
import java.util.*

class AlarmListener(private val textView: TextView) : Observer{ // TODO remove text view
    override fun update(observable: Observable?, args: Any?) {
        when(observable){
            is SoundDetector -> println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! SOUND ALARM !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            is MoveDetector ->  println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! MOVE  ALARM !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            is LocationService -> printLocation(args as Location)
            else ->             println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! UNDEF ALARM !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
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
}