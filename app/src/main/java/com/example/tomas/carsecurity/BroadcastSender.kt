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
        val intent = Intent(context.getString(R.string.utils_ui_update))

        intent.putExtra(context.getString(R.string.key_util_name), utilEnum.name)
        intent.putExtra(context.getString(R.string.key_util_activated), enabled)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun informUI(enabledUtils: Set<UtilsEnum>){
        for (util in enabledUtils){
            informUI(util, true)
        }
    }

    fun showMessage(msg: String) {
        val intent = Intent(context.getString(R.string.utils_ui_update))

        intent.putExtra(context.getString(R.string.key_show_message), msg)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

}