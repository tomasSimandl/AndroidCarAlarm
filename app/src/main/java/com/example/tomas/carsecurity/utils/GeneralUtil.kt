package com.example.tomas.carsecurity.utils

import java.util.*


abstract class GeneralUtil(private val utilsHelper: UtilsHelper) : Observer, Observable() {

    abstract val thisUtilEnum: UtilsEnum

    // UtilsThread
    abstract fun action(observable: Observable, args: Any?)

    override fun update(observable: Observable?, args: Any?) {
        if(observable != null) {
            val task = Runnable {
                this.action(observable, args)
            }
            utilsHelper.runOnUtilThread(task)
        }
    }

    abstract fun canEnable(): Boolean

    //MainServiceThread
    abstract fun enable() // !! Can be call more than once
    abstract fun disable(force: Boolean = false) // !! Can be call more than once// Force can be used only when application is closing
    abstract fun isEnabled(): Boolean
}