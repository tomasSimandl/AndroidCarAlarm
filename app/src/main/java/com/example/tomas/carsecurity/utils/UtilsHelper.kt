package com.example.tomas.carsecurity.utils

import android.content.SharedPreferences
import android.util.Log
import com.example.tomas.carsecurity.GeneralObservable
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.communication.CommunicationManager
import com.example.tomas.carsecurity.context.MyContext

class UtilsHelper (private val context: MyContext): SharedPreferences.OnSharedPreferenceChangeListener {
    private val tag = "utils.UtilsHelper"

    private val observablesMap: MutableMap<ObservableEnum, GeneralObservable> = HashMap()
    private val utilsMap: MutableMap<GeneralUtil, MutableSet<ObservableEnum>> = HashMap()

    private val workerThread = WorkerThread("UtilsThread")
    val communicationManager = CommunicationManager.getInstance(context.communicationContext)

    init {
        workerThread.start()
        workerThread.prepareHandler()
        context.utilsContext.registerOnPreferenceChanged(this)
    }

    fun destroy(){
        Log.d(tag, "Destroy")
        context.utilsContext.unregisterOnPreferenceChanged(this)
        workerThread.quit()
        communicationManager.destroy()
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {

        val task = Runnable {
            synchronized(this) {
                for (observer in observablesMap.values) {
                    if (observer.isEnable()) {
                        if (!observer.canEnable()) observer.disable()
                    } else {
                        if (observer.countObservers() > 0) observer.enable()
                    }
                }

                for (util in utilsMap.keys) {
                    if (util.isEnabled() && !util.canEnable()) {
                        util.disable()
                    }
                }
            }
        }
        workerThread.postTask(task)
    }

    fun runOnUtilThread(runnable: Runnable){
        Log.d(tag, """Adding task to ${workerThread.name} thread""")
        workerThread.postTask(runnable)
    }

    fun registerObserver(observableEnum: ObservableEnum, util: GeneralUtil): Boolean{
        synchronized(this) {

            Log.d(tag, """Registering observer $util to observable $observableEnum""")

            if (utilsMap[util]?.contains(observableEnum) == true) {
                Log.d(tag, """Observer $util is already registered in $observableEnum""")
                return true // already registered
            }

            var observable: GeneralObservable? = observablesMap[observableEnum]
            if (observable == null) {
                observable = observableEnum.getInstance(context)
                observablesMap[observableEnum] = observable
            }

            observable.addObserver(util)


            if (utilsMap[util] == null) {
                utilsMap[util] = HashSet()
            }
            utilsMap[util]!!.add(observableEnum)

            return true
        }
    }

    fun unregisterAllObservables(util: GeneralUtil){
        synchronized(this) {
            Log.d(tag, """Un-registering observer $util from all observables.""")

            val enums = utilsMap[util] ?: return

            for (observable in enums) {
                observablesMap[observable]?.deleteObserver(util)
            }
            enums.clear()
        }
    }

    fun unregisterObservable(observableEnum: ObservableEnum, util: GeneralUtil) {
        synchronized(this) {
            Log.d(tag, """Un-registering observer $util from observable $observableEnum""")

            if (utilsMap[util]?.contains(observableEnum) != true) {
                Log.d(tag, """Observer $util is not registered in observable $observableEnum""")
                return
            }

            observablesMap[observableEnum]?.deleteObserver(util)
            Log.d(tag, """Observer $util was un-registered from observable $observableEnum""")
        }
    }
}
