package com.example.tomas.carsecurity.utils

import com.example.tomas.carsecurity.BroadcastSender
import com.example.tomas.carsecurity.context.MyContext
import java.util.*

class UtilsManager(private val context: MyContext, private val broadcastSender: BroadcastSender): Observer {

    private val tag = "utils.UtilsManager"

    private val utilsHelper = UtilsHelper(context)

    private val utilsMap: MutableMap<UtilsEnum, GeneralUtil> = HashMap()

    override fun update(observable: Observable, args: Any) {

        if(observable is GeneralUtil && args is Boolean) {
            broadcastSender.informUI(observable.thisUtilEnum, args)
        }
    }

    private fun getGenericUtil(utilEnum: UtilsEnum): GeneralUtil{
        if(utilsMap[utilEnum] == null){
            utilsMap[utilEnum] = utilEnum.getInstance(context, utilsHelper)
            utilsMap[utilEnum]!!.addObserver(this)
        }

        return utilsMap[utilEnum] as GeneralUtil
    }

    fun switchUtil(utilEnum: UtilsEnum): Boolean {
        // task run sequentially in one thread

        val util: GeneralUtil = getGenericUtil(utilEnum)

        return if (util.isEnabled()) {
            util.disable()
        } else {
            util.enable()
        }
    }

    fun activateUtil(utilEnum: UtilsEnum): Boolean{
        val util: GeneralUtil = getGenericUtil(utilEnum)
        return util.enable()
    }

    fun deactivateUtil(utilEnum: UtilsEnum): Boolean{
        val util: GeneralUtil = getGenericUtil(utilEnum)
        return util.disable()
    }

    fun isAnyUtilEnabled(): Boolean{
        for (util in utilsMap.values){
            if(util.isEnabled()) return true
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
}