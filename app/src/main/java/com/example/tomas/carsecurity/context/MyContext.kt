package com.example.tomas.carsecurity.context

import android.content.Context
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.utils.UtilsManager
import java.util.*

class MyContext(val appContext: Context ) : Observable() {

    /** Contains private shared preferences which are shared across application. */
    private val sharedPreferences = appContext.getSharedPreferences(
            appContext.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE)

    val moveDetectorContext = MoveDetectorContext(sharedPreferences, appContext)
    val soundDetectorContext = SoundDetectorContext(sharedPreferences, appContext)
    val smsProviderContext = SmsProviderContext(sharedPreferences, appContext)
    val locationProviderContext = LocationProviderContext(sharedPreferences, appContext)

    val utilsManagerContext = UtilsManagerContext(sharedPreferences, appContext)
    val alarmContext = AlarmContext(sharedPreferences, appContext)


    fun updateContext(){
        setChanged()
        notifyObservers()
    }
}