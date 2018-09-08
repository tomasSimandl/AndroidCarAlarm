package com.example.tomas.carsecurity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import com.example.tomas.carsecurity.utils.UtilsEnum
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener  {


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
        setSupportActionBar(toolbar)

        // floating button
        button_float_setting.setOnClickListener {
            openSettings()
        }

        actionAlarm.setOnClickListener {
            sendIntentSwitchUtil(UtilsEnum.Alarm)
        }

        actionTracker.setOnClickListener{
            sendIntentSwitchUtil(UtilsEnum.Tracker)
        }

        // side panel initialization
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_panel_open, R.string.navigation_panel_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
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

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_settings -> {
                openSettings()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
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
