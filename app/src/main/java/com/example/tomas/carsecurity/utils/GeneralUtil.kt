package com.example.tomas.carsecurity.utils

import java.util.*


abstract class GeneralUtil(private val utilsHelper: UtilsHelper) : Observer, Observable() {

    abstract val thisUtilEnum: UtilsEnum

    abstract fun action(observable: Observable, args: Any?)

    override fun update(observable: Observable?, args: Any?) {
        if(observable != null) {
            val task = Runnable {
                this.action(observable, args)
            }
            utilsHelper.runOnUtilThread(task)
        }
    }

    abstract fun enable() // !! Can be call more than once
    abstract fun disable() // !! Can be call more than once
    abstract fun isEnabled(): Boolean
}