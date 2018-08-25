package com.example.tomas.carsecurity.utils

import com.example.tomas.carsecurity.context.MyContext

class UtilsManager(private val context: MyContext) {

    private val tag = "utils.UtilsManager"

    private val utilsHelper = UtilsHelper(context)

    private val utilsMap: MutableMap<UtilsEnum, GeneralUtil> = HashMap()


    fun switchUtil(utilEnum: UtilsEnum): Boolean {
        // task run sequentially in one thread

        if(utilsMap[utilEnum] == null){
            utilsMap[utilEnum] = utilEnum.getInstance(context, utilsHelper)
        }
        val util: GeneralUtil = utilsMap[utilEnum] as GeneralUtil


        return if (util.isEnabled()) {
            util.disable()
        } else {
            util.enable()
        }
    }

    fun isAnyUtilEnabled(): Boolean{
        var isAnyEnabled = false
        for (util in utilsMap.values){
            isAnyEnabled = isAnyEnabled || util.isEnabled()
            if(isAnyEnabled) break
        }

        return isAnyEnabled
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