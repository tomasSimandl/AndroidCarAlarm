package com.example.tomas.carsecurity.tools

import java.util.*


abstract class GeneralTool(private val toolsHelper: ToolsHelper) : Observer, Observable() {

    abstract val thisUtilEnum: ToolsEnum

    // UtilsThread
    abstract fun action(observable: Observable, args: Any?)

    override fun update(observable: Observable?, args: Any?) {
        if(observable != null) {
            val task = Runnable {
                this.action(observable, args)
            }
            toolsHelper.runOnUtilThread(task)
        }
    }

    abstract fun canEnable(): Boolean

    //MainServiceThread
    abstract fun enable() // !! Can be call more than once
    abstract fun disable(force: Boolean = false) // !! Can be call more than once// Force can be used only when application is closing
    abstract fun isEnabled(): Boolean
}