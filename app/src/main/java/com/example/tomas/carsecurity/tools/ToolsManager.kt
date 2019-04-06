package com.example.tomas.carsecurity.tools

import android.util.Log
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.BatteryUtil
import java.util.*
import kotlin.collections.HashMap

/**
 * Class is used for creating and stopping tools.
 *
 * @param context is my context used mainly for access shared preferences.
 */
class ToolsManager(private val context: MyContext) : Observer, Observable() {

    /** Logger tag */
    private val tag = "tools.ToolsManager"

    /** Instance of [ToolsHelper] used for connect sensors and tools. */
    private val toolsHelper = ToolsHelper(context)

    /** List of tools identifies by their enums */
    private val toolsMap: MutableMap<ToolsEnum, GeneralTool> = HashMap()

    /** Utils which are activate in all application runtime even if service is not foreground */
    private val defaultUtils = arrayOf(ToolsEnum.Battery)

    /**
     * Run initialization of default tools.
     */
    init {
        activateDefaultUtils()
    }

    /**
     * Method activate all utils defined by [defaultUtils] variable.
     */
    private fun activateDefaultUtils() {
        for (util in defaultUtils) {
            activateUtil(util)
        }
    }

    /**
     * Destroy all existing utils and [toolsHelper].
     */
    fun destroy() {
        Log.d(tag, "Destroy")

        for (util in toolsMap.values) {
            util.deleteObservers()
            if (util.isEnabled()) util.disable(true)
        }

        // destroy after all tools are disabled
        toolsHelper.destroy()
    }

    /**
     * Method respond on notifications from tools. Notification is send next by calling
     * [notifyObservers] method. When all observers all disabled inform [MainService] to
     * stop Foreground mode. Body of this method always run in MainServiceThread.
     *
     * @param observable which calls [notifyObservers] method
     * @param args additional arguments.
     */
    override fun update(observable: Observable, args: Any) {

        val task = Runnable {
            assert(Thread.currentThread().name == "MainServiceThread")

            if (observable is GeneralTool) {
                when (args) {
                    is Boolean -> {
                        setChanged()
                        notifyObservers(Pair(observable.thisToolEnum, args))

                        if (!isAnyUtilEnabled()) {
                            setChanged()
                            notifyObservers(MainService.Actions.ActionForegroundStop)
                        }
                    }
                    is String,
                    is MainService.Actions -> {
                        setChanged()
                        notifyObservers(args)
                    }
                }
            }
        }

        if (Thread.currentThread() != context.mainServiceThreadLooper.thread) {
            (context.mainServiceThreadLooper.thread as WorkerThread).postTask(task)
        } else {
            task.run()
        }
    }

    /**
     * Method change status of tool.
     * enable -> disable
     * disable -> enable
     *
     * @param utilEnum identifies tool of which status will be changed.
     */
    fun switchUtil(utilEnum: ToolsEnum) {
        // tasks are running sequentially in one thread
        val tool: GeneralTool = getGenericUtil(utilEnum)

        if (tool.isEnabled()) {
            tool.disable()
        } else {
            tool.enable()
        }
    }

    /**
     * Method enable tool given by [utilEnum]
     *
     * @param utilEnum identifies tool which will be enabled.
     */
    fun activateUtil(utilEnum: ToolsEnum) {
        getGenericUtil(utilEnum).enable()
    }

    /**
     * Method disable tool given by [utilEnum]
     *
     * @param utilEnum identifies tool which will be disabled.
     */
    fun deactivateUtil(utilEnum: ToolsEnum) {
        getGenericUtil(utilEnum).disable()
    }

    /**
     * Method check if there is any enabled util. Default utils are an exception which is not
     * counted.
     *
     * @return true when there is any non default enabled util.
     */
    fun isAnyUtilEnabled(): Boolean {
        for (util in toolsMap.values) {
            if (!defaultUtils.contains(util.thisToolEnum) && util.isEnabled()) return true
        }
        return false
    }

    /**
     * Method return set of enabled tools.
     *
     * @return set of enabled tools given by their enums.
     */
    fun getEnabledUtils(): Set<ToolsEnum> {
        val enabledUtils: MutableSet<ToolsEnum> = HashSet()

        for (util in toolsMap.keys) {
            if (toolsMap[util]?.isEnabled() == true) {
                enabledUtils.add(util)
            }
        }
        return enabledUtils
    }

    /**
     * Method return if alarm is triggered.
     *
     * @return if alarm is triggered.
     */
    fun isAlarm(): Boolean {
        for (util in toolsMap.values) {
            if (util is Alarm){
                return util.isAlarm
            }
        }
        return false
    }

    /**
     * Method get instance of input tool. Instance is taken form [toolsMap] or is created.
     * This class is registered to observe created tool.
     *
     * @param utilEnum which identifies tool of which instance is required.
     * @return requested instance.
     */
    private fun getGenericUtil(utilEnum: ToolsEnum): GeneralTool {
        if (toolsMap[utilEnum] == null) {
            toolsMap[utilEnum] = utilEnum.getInstance(context, toolsHelper)
            toolsMap[utilEnum]!!.addObserver(this)
        }

        return toolsMap[utilEnum] as GeneralTool
    }

    /**
     * Method collect application status and send it via communication provider given by
     * [communicatorHash].
     *
     * @param communicatorHash is hash of communication provider which will be used for sending
     *          status message.
     */
    fun sendStatus(communicatorHash: Int) {
        Log.d(tag, "Command to send status to communicator with hash: $communicatorHash")
        val tools: MutableMap<ToolsEnum, Boolean> = HashMap()
        toolsMap.forEach { tools[it.key] = it.value.isEnabled() }

        val powerSaveMode = context.toolsContext.isPowerSaveMode

        val batteryStatus = BatteryUtil.getBatteryStatus(context.appContext)
        val isCharging = batteryStatus.second
        val batteryPct = batteryStatus.first

        toolsHelper.communicationManager.sendStatus(
                communicatorHash, batteryPct, isCharging, powerSaveMode, tools)
    }
}