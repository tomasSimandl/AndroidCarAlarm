package com.example.tomas.carsecurity.utils

import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.context.MyContext
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class UtilsManager(private val context: MyContext) {

    private val workerThread = WorkerThread("UtilsThread")

    private val observablesMap: MutableMap<ObservableEnum, GeneralObservable> = HashMap()

    private val utilsMap: MutableMap<GeneralUtil, MutableSet<ObservableEnum>> = HashMap()

    init {
        workerThread.start()
        workerThread.prepareHandler()
    }

    fun addUtilsTask(util: GeneralUtil, observable: Observable, any: Any?){
        val task = Runnable {
            util.action(observable, any)
        }
        workerThread.postTask(task)
    }

    fun destroy(){
        workerThread.quit()
    }





    fun registerObserver(observableEnum: ObservableEnum, util: GeneralUtil): Boolean{

        if(!observableEnum.isAvailable(context.utilsManagerContext)){
            return false // observable is not available (disabled by users setting)
        }

        if(utilsMap[util]?.contains(observableEnum) == true){
            return true // already registered
        }

        var observable: GeneralObservable? = observablesMap[observableEnum]
        if(observable == null){
            observable = observableEnum.getInstance(context)
            observablesMap[observableEnum] = observable
        }

        observable.addObserver(util)


        if(utilsMap[util] == null){
            utilsMap[util] = HashSet()
        }
        utilsMap[util]!!.add(observableEnum)

        return true
    }

    fun unregisterAllObservables(util: GeneralUtil){

        val enums = utilsMap[util] ?: return

        for (observable in enums){
            observablesMap[observable]?.deleteObserver(util)
        }
    }
}