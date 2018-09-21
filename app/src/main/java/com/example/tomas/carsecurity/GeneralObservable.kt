package com.example.tomas.carsecurity

import java.util.*

/**
 * This class is used as parent of all detectors. Every child must override methods [disable],
 * [enable] and [isEnable]. Class GeneralObservable using class [Observable] for notification of
 * any class which implements interface [Observer]. When some action is detected child detector must
 * call methods [setChanged] and [notifyObservers] in this order.
 */
abstract class GeneralObservable : Observable() {

    abstract fun canEnable(): Boolean

    /** Method is used for deactivating of detector. Method is automatically call when last listener is unregistered. */
    protected abstract fun disable()
    /** Method is used for activating detector. Method is automatically call when first listener is registered. */
    protected abstract fun enable()
    /** Method only returns if detector is enabled */
    abstract fun isEnable() :Boolean

    /**
     * Method is used for registration of all Observers.
     * When first observer is registered. Method [enable] is automatically called.
     *
     * @param observer which will be registered.
    */
    override fun addObserver(observer: Observer) {
        if(countObservers() == 0) enable()

        super.addObserver(observer)
    }

    /**
     * Method is used for un-registration of observers.
     * When last listener is unregistered. Method [disable] is automatically called.
     *
     * @param observer which will be unregistered.
     */
    override fun deleteObserver(observer: Observer?) {
        super.deleteObserver(observer)

        if(countObservers() == 0) disable()
    }

    /**
     * Method is used for un-registration of all observers.
     * Method [disable] is automatically called.
     */
    override fun deleteObservers() {
        super.deleteObservers()

        disable()
    }
}