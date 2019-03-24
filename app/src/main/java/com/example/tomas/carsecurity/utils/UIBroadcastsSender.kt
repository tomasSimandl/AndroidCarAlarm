package com.example.tomas.carsecurity.utils

import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.example.tomas.carsecurity.fragments.MainFragment
import com.example.tomas.carsecurity.tools.ToolsEnum

/**
 * Class used for sending Broadcast messages to [MainFragment] to update GUI.
 */
class UIBroadcastsSender(private val context: Context) {

    /** Logger tag */
    private val tag = "UIBroadcastsSender"

    /**
     * Method send Broadcast to [MainFragment] to inform GUI about tool status changed.
     *
     * @param utilEnum identification of tool.
     * @param enabled true if tool was activated, false when tool was deactivated.
     */
    fun informUI(utilEnum: ToolsEnum, enabled: Boolean) {
        Log.d(tag, """Sending information about util to UI. Util: ${utilEnum.name} is
            |${if (enabled) "enabled" else "disabled"}.""".trimMargin())

        val intent = Intent(MainFragment.BroadcastKeys.BroadcastUpdateUI.name)

        intent.putExtra(MainFragment.BroadcastKeys.KeyUtilName.name, utilEnum.name)
        intent.putExtra(MainFragment.BroadcastKeys.KeyUtilActivated.name, enabled)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    /**
     * Method send Broadcast list of activated tools to [MainFragment].
     *
     * @param enabledUtils set of enabled tools.
     */
    fun informUI(enabledUtils: Set<ToolsEnum>) {
        for (util in enabledUtils) {
            informUI(util, true)
        }
    }

    /**
     * Method send error message to to [MainFragment].
     *
     * @param msg which should be displayed to user.
     */
    fun showMessage(msg: String) {
        val intent = Intent(MainFragment.BroadcastKeys.BroadcastUpdateUI.name)

        intent.putExtra(MainFragment.BroadcastKeys.KeyShowMessage.name, msg)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}