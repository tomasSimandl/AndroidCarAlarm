package com.example.tomas.carsecurity.context

import android.content.Context
import android.os.Looper
import com.example.tomas.carsecurity.R
import java.util.*

class MyContext(val appContext: Context, val mainServiceThreadLooper: Looper) : Observable() {

    /** Contains private shared preferences which are shared across application. */
    val sharedPreferences = appContext.getSharedPreferences(
            appContext.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE)



    fun updateContext(){
        setChanged()
        notifyObservers()
    }
}