package com.example.tomas.carsecurity.utils

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.context.MyContext
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class UtilsManager(private val context: MyContext) {

    private val tag = "utils.UtilsManager"

    private val workerThread = WorkerThread("UtilsThread")

    private val observablesMap: MutableMap<ObservableEnum, GeneralObservable> = HashMap()

    private val utilsMap: MutableMap<GeneralUtil, MutableSet<ObservableEnum>> = HashMap()

    init {
        workerThread.start()
        workerThread.prepareHandler()
    }

    fun runOnUtilThread(runnable: Runnable){
        Log.d(tag, """Adding task to ${workerThread.name} thread""")
        workerThread.postTask(runnable)
    }

    fun addUtilsTask(util: GeneralUtil, observable: Observable, any: Any?){
        val task = Runnable {
            util.action(observable, any)
        }
        runOnUtilThread(task)
    }

    fun destroy(){
        Log.d(tag, "Destroying workerThread.")
        workerThread.quit() // TODO use this 'destroy' method
    }


    fun informUI(util: GeneralUtil, enabled: Boolean) {
        Log.d(tag, """Sending information about util to UI. Util: $util is ${if(enabled) "enabled" else "disabled"}.""")
        val intent = Intent(context.appContext.getString(R.string.utils_ui_update))

        intent.putExtra(context.appContext.getString(R.string.key_util_name), util::class.java.canonicalName)
        intent.putExtra(context.appContext.getString(R.string.key_util_activated), enabled)
        LocalBroadcastManager.getInstance(context.appContext).sendBroadcast(intent)
    }

    fun informUI(){
        for (util in utilsMap.keys){
            informUI(util, util.isEnabled())
        }
    }

    fun isAnyUtilEnabled(): Boolean{
        var isAnyEnabled = false
        for (util in utilsMap.keys){
            isAnyEnabled = isAnyEnabled || util.isEnabled()
            if(isAnyEnabled) break
        }

        return isAnyEnabled
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