package com.example.tomas.carsecurity

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import com.example.tomas.carsecurity.context.UtilsContext
import com.example.tomas.carsecurity.utils.UtilsEnum
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener  {

    enum class BroadcastKeys{
        BroadcastUpdateUI, KeyShowMessage, KeyUtilName, KeyUtilActivated
    }

    private val tag = "MainActivity"
    private var isProgressRun = false

    private lateinit var utilsContext: UtilsContext

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "BroadcastReceiver.onReceive was triggered.")

            if (intent.hasExtra(BroadcastKeys.KeyShowMessage.name)) {

                AlertDialog.Builder(this@MainActivity)
                        .setMessage(intent.getStringExtra(BroadcastKeys.KeyShowMessage.name))
                        .setTitle(R.string.error_msg_title)
                        .setPositiveButton(R.string.ok, null)
                        .create().show()

            } else {
                val utilName = intent.getStringExtra(BroadcastKeys.KeyUtilName.name)
                val utilEnabled = intent.getBooleanExtra(BroadcastKeys.KeyUtilActivated.name, false)

                when (utilName) {
                    UtilsEnum.Alarm.name -> {
                        if(utilEnabled){
                            runProgress(context)
                        } else {
                            progressBar.max = 0
                        }
                        changeColor(actionAlarm, utilEnabled)
                    }
                    UtilsEnum.Tracker.name -> changeColor(actionTracker, utilEnabled)
                }
            }
        }
    }

    private fun runProgress(context: Context) {
        progressBar.max = UtilsContext(context).startAlarmInterval / 1000
        progressBar.progress = 0

        if(!isProgressRun) {
            isProgressRun = true
            progressBar.visibility = ProgressBar.VISIBLE

            val handler = Handler()

            Thread(Runnable {
                while (progressBar.max > progressBar.progress) {
                    handler.post {
                        progressBar.progress++
                    }

                    Thread.sleep(1000)
                }

                handler.post {
                    progressBar.visibility = ProgressBar.GONE
                    isProgressRun = false
                }
            }).start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        utilsContext = UtilsContext(applicationContext)
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
        broadcastManager.registerReceiver(receiver, IntentFilter(BroadcastKeys.BroadcastUpdateUI.name))

        changeColor(actionTracker, false)
        changeColor(actionAlarm, false)

        sendIntent(MainService.Actions.ActionStatusUI.name)

        setVisibility(actionAlarm, utilsContext.isAlarmAllowed)
        setVisibility(actionTracker, utilsContext.isTrackerAllowed)
        setVisibility(power_save_indication, utilsContext.isPowerSaveMode)
        utilsContext.registerOnPreferenceChanged(this)
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        utilsContext.unregisterOnPreferenceChanged(this)

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

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        when (key){
            getString(R.string.key_tool_alarm_is_allowed) -> setVisibility(actionAlarm, utilsContext.isAlarmAllowed)
            getString(R.string.key_tool_tracker_is_allowed) -> setVisibility(actionTracker, utilsContext.isTrackerAllowed)
            getString(R.string.key_tool_battery_mode) -> {
                setVisibility(actionAlarm, utilsContext.isAlarmAllowed)
                setVisibility(actionTracker, utilsContext.isTrackerAllowed)
                setVisibility(power_save_indication, utilsContext.isPowerSaveMode)
            }
        }
    }

    private fun setVisibility(button: View, visible: Boolean) {
        button.visibility = if(visible){
            Button.VISIBLE
        } else {
            Button.GONE
        }
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
