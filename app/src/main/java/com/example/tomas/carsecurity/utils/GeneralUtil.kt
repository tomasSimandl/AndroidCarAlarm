package com.example.tomas.carsecurity.utils

import com.example.tomas.carsecurity.context.MyContext
import java.util.*


abstract class GeneralUtil(private val context: MyContext, private val utilsManager: UtilsManager) : Observer {

    abstract fun action(observable: Observable, args: Any?)

    override fun update(observable: Observable?, args: Any?) {
        if(observable != null)
            utilsManager.addUtilsTask(this, observable, args)
    }

    abstract fun enable()
    abstract fun disable()
    abstract fun isEnabled(): Boolean
}