package com.example.tomas.carsecurity.tools

import android.content.SharedPreferences
import android.util.Log
import com.example.tomas.carsecurity.ObservableEnum
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.communication.CommunicationManager
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.GeneralObservable

/**
 * Class used for managing registration and unregistering sensors from tools.
 *
 * @param context is my context used for access shared preferences.
 */
class ToolsHelper(private val context: MyContext) : SharedPreferences.OnSharedPreferenceChangeListener {

    /** Logger tag */
    private val tag = "tools.ToolsHelper"

    /** Map of instanced sensors identifies by their enums */
    private val observablesMap: MutableMap<ObservableEnum, GeneralObservable> = HashMap()
    /** Map which contains list of sensors specific to tool */
    private val toolsMap: MutableMap<GeneralTool, MutableSet<ObservableEnum>> = HashMap()

    /** Thread where are running tools operations */
    private val workerThread = WorkerThread("UtilsThread")
    /** Instance of [CommunicationManager] used for sending information to user */
    val communicationManager = CommunicationManager.getInstance(context.communicationContext)

    /**
     * Initialization of workerThread
     */
    init {
        workerThread.start()
        workerThread.prepareHandler()
        context.toolsContext.registerOnPreferenceChanged(this)
    }

    /**
     * Cleaning all necessary initialized variables.
     */
    fun destroy() {
        Log.d(tag, "Destroy")
        context.toolsContext.unregisterOnPreferenceChanged(this)
        workerThread.quit()
        communicationManager.destroy()
    }

    /**
     * Method is automatically called when any value in sharedPreferences is changed. On value
     * change method check if all enabled sensors and tools can be still enabled and disable them
     * when it is needed. Method is synchronized to this object.
     *
     * @param p0 sharedPreferences storage where some value was changed.
     * @param key is key of preference which was changed.
     */
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

                for (util in toolsMap.keys) {
                    if (util.isEnabled() && !util.canEnable()) {
                        util.disable()
                    }
                }
            }
        }
        workerThread.postTask(task)
    }

    /**
     * Method run input task in UtilsThread thread.
     * @param runnable is runnable task which will be run in UtilsThread.
     */
    fun runOnUtilThread(runnable: Runnable) {
        Log.d(tag, """Adding task to ${workerThread.name} thread""")
        workerThread.postTask(runnable)
    }

    /**
     * Method register required sensor to given tool. Method check is sensor is already registered.
     * Sensors which are already used in other tools are reused. Method is synchronized to
     * this object
     *
     * @param observableEnum enum of required sensor.
     * @param tool to which will be registered sensor.
     * @return true when registration was successful or when sensor is already registered.
     */
    fun registerObserver(observableEnum: ObservableEnum, tool: GeneralTool): Boolean {
        synchronized(this) {

            Log.d(tag, """Registering observer $tool to observable $observableEnum""")

            if (toolsMap[tool]?.contains(observableEnum) == true) {
                Log.d(tag, """Observer $tool is already registered in $observableEnum""")
                return true // already registered
            }

            var observable: GeneralObservable? = observablesMap[observableEnum]
            if (observable == null) {
                observable = observableEnum.getInstance(context)
                observablesMap[observableEnum] = observable
            }

            observable.addObserver(tool)


            if (toolsMap[tool] == null) {
                toolsMap[tool] = HashSet()
            }
            toolsMap[tool]!!.add(observableEnum)

            return true
        }
    }

    /**
     * Method unregister all sensors from given tool. Method is synchronized with this object.
     * @param tool of which will be unregistered all sensors.
     */
    fun unregisterAllObservables(tool: GeneralTool) {
        synchronized(this) {
            Log.d(tag, """Un-registering observer $tool from all observables.""")

            val enums = toolsMap[tool] ?: return

            for (observable in enums) {
                observablesMap[observable]?.deleteObserver(tool)
            }
            enums.clear()
        }
    }

    /**
     * Method unregister given sensor from given tool. Method is synchronized with this object.
     *
     * @param observableEnum enum of sensor which will be unregistered.
     * @param tool in which will be sensor unregistered.
     */
    fun unregisterObservable(observableEnum: ObservableEnum, tool: GeneralTool) {
        synchronized(this) {
            Log.d(tag, """Un-registering observer $tool from observable $observableEnum""")

            if (toolsMap[tool]?.contains(observableEnum) != true) {
                Log.d(tag, """Observer $tool is not registered in observable $observableEnum""")
                return
            }

            observablesMap[observableEnum]?.deleteObserver(tool)
            Log.d(tag, """Observer $tool was un-registered from observable $observableEnum""")
        }
    }
}
