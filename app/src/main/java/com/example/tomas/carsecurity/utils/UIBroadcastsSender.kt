package com.example.tomas.carsecurity.utils

import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.example.tomas.carsecurity.fragments.MainFragment
import com.example.tomas.carsecurity.tools.ToolsEnum

class UIBroadcastsSender(private val context: Context) {

    private val tag = "UIBroadcastsSender"

    fun informUI(utilEnum: ToolsEnum, enabled: Boolean) {
        Log.d(tag, """Sending information about util to UI. Util: ${utilEnum.name} is ${if(enabled) "enabled" else "disabled"}.""")
        val intent = Intent(MainFragment.BroadcastKeys.BroadcastUpdateUI.name)

        intent.putExtra(MainFragment.BroadcastKeys.KeyUtilName.name, utilEnum.name)
        intent.putExtra(MainFragment.BroadcastKeys.KeyUtilActivated.name, enabled)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun informUI(enabledUtils: Set<ToolsEnum>){
        for (util in enabledUtils){
            informUI(util, true)
        }
    }

    fun showMessage(msg: String) {
        val intent = Intent(MainFragment.BroadcastKeys.BroadcastUpdateUI.name)

        intent.putExtra(MainFragment.BroadcastKeys.KeyShowMessage.name, msg)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

}