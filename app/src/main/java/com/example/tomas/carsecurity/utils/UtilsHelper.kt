package com.example.tomas.carsecurity.utils

import android.util.Log
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.communication.CommunicationManager
import com.example.tomas.carsecurity.context.MyContext

class UtilsHelper (private val context: MyContext) {

    private val tag = "utils.UtilsHelper"

    private val workerThread = WorkerThread("UtilsThread")

    private val utilsMap: MutableMap<GeneralUtil, MutableSet<ObservableEnum>> = HashMap()

    private val observablesMap: MutableMap<ObservableEnum, GeneralObservable> = HashMap()

    val communicationManager = CommunicationManager()



    init {
        workerThread.start()
        workerThread.prepareHandler()
    }

    fun destroy(){
        Log.d(tag, "Destroying workerThread.")
        workerThread.quit() // TODO use this 'destroy' method
    }

    fun runOnUtilThread(runnable: Runnable){
        Log.d(tag, """Adding task to ${workerThread.name} thread""")
        workerThread.postTask(runnable)
    }

    fun registerObserver(observableEnum: ObservableEnum, util: GeneralUtil): Boolean{

        Log.d(tag, """Registering observer $util to observable $observableEnum""")

        if(!observableEnum.isAvailable(context.utilsManagerContext)){
            Log.d(tag, "Observable is not available.")
            return false // observable is not available (disabled by users setting)
        }

        if(utilsMap[util]?.contains(observableEnum) == true){
            Log.d(tag, """Observer $util is already registered in $observableEnum""")
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

        Log.d(tag, """Unregistering observer $util from all observables.""")

        val enums = utilsMap[util] ?: return

        for (observable in enums){
            observablesMap[observable]?.deleteObserver(util)
        }
    }
}
