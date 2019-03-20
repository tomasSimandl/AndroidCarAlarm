package com.example.tomas.carsecurity.tools

import android.content.SharedPreferences
import android.util.Log
import com.example.tomas.carsecurity.sensors.GeneralObservable
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.communication.CommunicationManager
import com.example.tomas.carsecurity.context.MyContext

class ToolsHelper (private val context: MyContext): SharedPreferences.OnSharedPreferenceChangeListener {
    private val tag = "tools.ToolsHelper"

    private val observablesMap: MutableMap<ObservableEnum, GeneralObservable> = HashMap()
    private val utilsMap: MutableMap<GeneralTool, MutableSet<ObservableEnum>> = HashMap()

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

    fun registerObserver(observableEnum: ObservableEnum, tool: GeneralTool): Boolean{
        synchronized(this) {

            Log.d(tag, """Registering observer $tool to observable $observableEnum""")

            if (utilsMap[tool]?.contains(observableEnum) == true) {
                Log.d(tag, """Observer $tool is already registered in $observableEnum""")
                return true // already registered
            }

            var observable: GeneralObservable? = observablesMap[observableEnum]
            if (observable == null) {
                observable = observableEnum.getInstance(context)
                observablesMap[observableEnum] = observable
            }

            observable.addObserver(tool)


            if (utilsMap[tool] == null) {
                utilsMap[tool] = HashSet()
            }
            utilsMap[tool]!!.add(observableEnum)

            return true
        }
    }

    fun unregisterAllObservables(tool: GeneralTool){
        synchronized(this) {
            Log.d(tag, """Un-registering observer $tool from all observables.""")

            val enums = utilsMap[tool] ?: return

            for (observable in enums) {
                observablesMap[observable]?.deleteObserver(tool)
            }
            enums.clear()
        }
    }

    fun unregisterObservable(observableEnum: ObservableEnum, tool: GeneralTool) {
        synchronized(this) {
            Log.d(tag, """Un-registering observer $tool from observable $observableEnum""")

            if (utilsMap[tool]?.contains(observableEnum) != true) {
                Log.d(tag, """Observer $tool is not registered in observable $observableEnum""")
                return
            }

            observablesMap[observableEnum]?.deleteObserver(tool)
            Log.d(tag, """Observer $tool was un-registered from observable $observableEnum""")
        }
    }
}
