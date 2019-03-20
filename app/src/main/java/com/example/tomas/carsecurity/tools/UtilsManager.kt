package com.example.tomas.carsecurity.tools

import android.util.Log
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.WorkerThread
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.BatteryUtil
import java.util.*
import kotlin.collections.HashMap

class UtilsManager(private val context: MyContext, reload: Boolean): Observer, Observable() {

    private val tag = "utils.UtilsManager"

    private val utilsHelper = UtilsHelper(context)

    private val utilsMap: MutableMap<UtilsEnum, GeneralUtil> = HashMap()

    // Utils which are activate in all application runtime even if service is not foreground
    private val defaultUtils = arrayOf(UtilsEnum.Battery)

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

        for(util in utilsMap.values){
            util.deleteObservers()
            if(util.isEnabled()) util.disable(true)
        }

        // destroy after all utils are disabled
        utilsHelper.destroy()
    }

    override fun update(observable: Observable, args: Any) {

        val task = Runnable {
            assert(Thread.currentThread().name == "MainServiceThread")

            if (observable is GeneralUtil) {
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

    fun switchUtil(utilEnum: UtilsEnum) {
        // tasks are running sequentially in one thread
        val util: GeneralUtil = getGenericUtil(utilEnum)

        if (util.isEnabled()) {
            util.disable()
        } else {
            util.enable()
        }
    }

    fun activateUtil(utilEnum: UtilsEnum) {
        getGenericUtil(utilEnum).enable()
    }

    fun deactivateUtil(utilEnum: UtilsEnum) {
        getGenericUtil(utilEnum).disable()
    }

    fun isAnyUtilEnabled(): Boolean{
        for (util in utilsMap.values){
            if(!defaultUtils.contains(util.thisUtilEnum) && util.isEnabled()) return true
        }
        return false
    }

    fun getEnabledUtils() : Set<UtilsEnum> {
        val enabledUtils: MutableSet<UtilsEnum> = HashSet()

        for (util in utilsMap.keys){
            if(utilsMap[util]?.isEnabled() == true) {
                enabledUtils.add(util)
            }
        }
        return enabledUtils
    }

    private fun getGenericUtil(utilEnum: UtilsEnum): GeneralUtil{
        if(utilsMap[utilEnum] == null){
            utilsMap[utilEnum] = utilEnum.getInstance(context, utilsHelper)
            utilsMap[utilEnum]!!.addObserver(this)
        }

        return utilsMap[utilEnum] as GeneralUtil
    }

    fun sendStatus(communicatorHash: Int){
        Log.d(tag, "Command to send status to communicator with hash: $communicatorHash")
        val utils: MutableMap<UtilsEnum, Boolean> = HashMap()
        utilsMap.forEach { utils[it.key] = it.value.isEnabled() }

        val powerSaveMode = context.utilsContext.isPowerSaveMode

        val batteryStatus = BatteryUtil.getBatteryStatus(context.appContext)
        val isCharging = batteryStatus.second
        val batteryPct = batteryStatus.first

        utilsHelper.communicationManager.sendStatus(
                communicatorHash, batteryPct, isCharging, powerSaveMode, utils)
    }
}