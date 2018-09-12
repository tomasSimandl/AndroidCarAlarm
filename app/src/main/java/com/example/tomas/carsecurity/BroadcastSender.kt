package com.example.tomas.carsecurity

import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.example.tomas.carsecurity.utils.UtilsEnum

class BroadcastSender(private val context: Context) {

    private val tag = "BroadcastSender"

    fun informUI(utilEnum: UtilsEnum, enabled: Boolean) {
        Log.d(tag, """Sending information about util to UI. Util: ${utilEnum.name} is ${if(enabled) "enabled" else "disabled"}.""")
        val intent = Intent(MainActivity.BroadcastKeys.BroadcastUpdateUI.name)

        intent.putExtra(MainActivity.BroadcastKeys.KeyUtilName.name, utilEnum.name)
        intent.putExtra(MainActivity.BroadcastKeys.KeyUtilActivated.name, enabled)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun informUI(enabledUtils: Set<UtilsEnum>){
        for (util in enabledUtils){
            informUI(util, true)
        }
    }

    fun showMessage(msg: String) {
        val intent = Intent(MainActivity.BroadcastKeys.BroadcastUpdateUI.name)

        intent.putExtra(MainActivity.BroadcastKeys.KeyShowMessage.name, msg)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

}