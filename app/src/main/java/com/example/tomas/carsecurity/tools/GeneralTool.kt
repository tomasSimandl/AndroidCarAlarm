package com.example.tomas.carsecurity.tools

import java.util.*

/**
 * Based abstract class of every tool created in application.
 *
 * @param toolsHelper used mainly for registration of sensors
 */
abstract class GeneralTool(private val toolsHelper: ToolsHelper) : Observer, Observable() {

    /** Identification of the tool by [ToolsEnum] */
    abstract val thisToolEnum: ToolsEnum

    /**
     * Method which is called when observed sensors send any observation. This method is
     * triggered only by [update] method and run in UtilsThread thread.
     *
     * @param observable is [Observable] object which call [notifyObservers] method.
     * @param args are additional arguments which was transfer with observation.
     */
    abstract fun action(observable: Observable, args: Any?)

    /**
     * Method which is called by observed sensors. Method only call [action] method, but in
     * UtilsThread thread.
     *
     * @param observable is [Observable] object which call [notifyObservers] method.
     * @param args are additional arguments which was transfer with observation.
     */
    override fun update(observable: Observable?, args: Any?) {
        if (observable != null) {
            val task = Runnable {
                this.action(observable, args)
            }
            toolsHelper.runOnUtilThread(task)
        }
    }

    /**
     * Identification if tool can be enabled.
     * @return true when tool can be enabled.
     */
    abstract fun canEnable(): Boolean


    //MainServiceThread
    /**
     * Enable tool
     */
    abstract fun enable() // !! Can be call more than once

    /**
     * Disable tool. Force can be used only when application is closing.
     * @param force true if tool must be disabled
     */
    abstract fun disable(force: Boolean = false) // !! Can be call more than once

    /**
     * Indication if tool is enabled.
     * @return true when tool is enabled.
     */
    abstract fun isEnabled(): Boolean
}