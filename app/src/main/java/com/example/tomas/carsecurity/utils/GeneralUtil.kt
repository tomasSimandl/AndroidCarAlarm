package com.example.tomas.carsecurity.utils

import com.example.tomas.carsecurity.context.MyContext
import java.util.*


abstract class GeneralUtil(private val context: MyContext, private val utilsHelper: UtilsHelper) : Observer, Observable() {

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

    abstract fun enable(): Boolean // !! Can be call more than once
    abstract fun disable(): Boolean // !! Can be call more than once
    abstract fun isEnabled(): Boolean
}