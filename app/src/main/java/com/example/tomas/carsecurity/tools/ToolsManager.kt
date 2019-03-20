package com.example.tomas.carsecurity.tools

import android.util.Log
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.BatteryUtil
import java.util.*
import kotlin.collections.HashMap

class ToolsManager(private val context: MyContext, reload: Boolean): Observer, Observable() {

    private val tag = "tools.ToolsManager"

    private val utilsHelper = ToolsHelper(context)

    private val toolsMap: MutableMap<ToolsEnum, GeneralTool> = HashMap()

    // Utils which are activate in all application runtime even if service is not foreground
    private val defaultUtils = arrayOf(ToolsEnum.Battery)

    init {
        activateDefaultUtils()

        if (reload) {
            // TODO use reload for loading from ServiceState
        }
    }

    private fun activateDefaultUtils(){
        for (util in defaultUtils){
            activateUtil(util)
        }
    }

    fun destroy(){
        Log.d(tag, "Destroy")

        for(util in toolsMap.values){
            util.deleteObservers()
            if(util.isEnabled()) util.disable(true)
        }

        // destroy after all tools are disabled
        utilsHelper.destroy()
    }

    override fun update(observable: Observable, args: Any) {

        val task = Runnable {
            assert(Thread.currentThread().name == "MainServiceThread")

            if (observable is GeneralTool) {
                when (args) {
                    is Boolean -> {
                        setChanged()
                        notifyObservers(Pair(observable.thisUtilEnum, args))

                        if (!isAnyUtilEnabled()) {
                            setChanged()
                            notifyObservers(MainService.Actions.ActionForegroundStop)
                        }
                    }
                    is String -> {
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

    fun switchUtil(utilEnum: ToolsEnum) {
        // tasks are running sequentially in one thread
        val tool: GeneralTool = getGenericUtil(utilEnum)

        if (tool.isEnabled()) {
            tool.disable()
        } else {
            tool.enable()
        }
    }

    fun activateUtil(utilEnum: ToolsEnum) {
        getGenericUtil(utilEnum).enable()
    }

    fun deactivateUtil(utilEnum: ToolsEnum) {
        getGenericUtil(utilEnum).disable()
    }

    fun isAnyUtilEnabled(): Boolean{
        for (util in toolsMap.values){
            if(!defaultUtils.contains(util.thisUtilEnum) && util.isEnabled()) return true
        }
        return false
    }

    fun getEnabledUtils() : Set<ToolsEnum> {
        val enabledUtils: MutableSet<ToolsEnum> = HashSet()

        for (util in toolsMap.keys){
            if(toolsMap[util]?.isEnabled() == true) {
                enabledUtils.add(util)
            }
        }
        return enabledUtils
    }

    private fun getGenericUtil(utilEnum: ToolsEnum): GeneralTool{
        if(toolsMap[utilEnum] == null){
            toolsMap[utilEnum] = utilEnum.getInstance(context, utilsHelper)
            toolsMap[utilEnum]!!.addObserver(this)
        }

        return toolsMap[utilEnum] as GeneralTool
    }

    fun sendStatus(communicatorHash: Int){
        Log.d(tag, "Command to send status to communicator with hash: $communicatorHash")
        val tools: MutableMap<ToolsEnum, Boolean> = HashMap()
        toolsMap.forEach { tools[it.key] = it.value.isEnabled() }

        val powerSaveMode = context.utilsContext.isPowerSaveMode

        val batteryStatus = BatteryUtil.getBatteryStatus(context.appContext)
        val isCharging = batteryStatus.second
        val batteryPct = batteryStatus.first

        utilsHelper.communicationManager.sendStatus(
                communicatorHash, batteryPct, isCharging, powerSaveMode, tools)
    }
}