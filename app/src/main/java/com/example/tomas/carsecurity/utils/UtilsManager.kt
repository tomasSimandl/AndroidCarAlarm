package com.example.tomas.carsecurity.utils

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.R
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
        workerThread.quit() // TODO use method
    }


    fun informUI(util: GeneralUtil, enabled: Boolean) {
        val intent = Intent(context.appContext.getString(R.string.utils_ui_update))

//        val activatedKeys = mutableListOf<String>()
//        for (util in utilsMap.keys){
//            if(util.isEnabled()){
//                activatedKeys.add(util::class.java.canonicalName)
//            }
//        }
//        intent.putExtra(context.appContext.getString(R.string.key_util_activated_array), activatedKeys.toTypedArray())

        intent.putExtra(context.appContext.getString(R.string.key_util_name), util::class.java.canonicalName)
        intent.putExtra(context.appContext.getString(R.string.key_util_activated), enabled)
        LocalBroadcastManager.getInstance(context.appContext).sendBroadcast(intent)
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