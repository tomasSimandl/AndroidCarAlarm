package com.example.tomas.carsecurity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import com.example.tomas.carsecurity.utils.UtilsEnum
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "BroadcastReceiver.onReceive was triggered.")
            val utilName = intent.getStringExtra(getString(R.string.key_util_name))
            val utilEnabled = intent.getBooleanExtra(getString(R.string.key_util_activated), false)

            when (utilName) {
                UtilsEnum.Alarm.name -> changeColor(actionAlarm, utilEnabled)
                UtilsEnum.Tracker.name -> changeColor(actionTracker, utilEnabled)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        actionAlarm.setOnClickListener {
            sendIntentSwitchUtil(UtilsEnum.Alarm)
        }

        actionTracker.setOnClickListener{
            sendIntentSwitchUtil(UtilsEnum.Tracker)
        }
    }

    override fun onResume() {
        super.onResume()

        val broadcastManager = LocalBroadcastManager.getInstance(this)
        broadcastManager.registerReceiver(receiver, IntentFilter(getString(R.string.utils_ui_update)))

        sendIntent(MainService.Actions.ActionStatusUI.name)
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onPause()
    }


    private fun changeColor(button: Button, enabled: Boolean){
        if (enabled){
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.enabled))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.disabled))
        }
    }

    private fun sendIntent(action: String){
        val intent = Intent(applicationContext, MainService::class.java)
        intent.action = action
        startService(intent)
    }

    private fun sendIntentSwitchUtil(util: UtilsEnum){
        val intent = Intent(applicationContext, MainService::class.java)
        intent.action = MainService.Actions.ActionSwitchUtil.name
        intent.putExtra("util", util)
        startService(intent)
    }
}
