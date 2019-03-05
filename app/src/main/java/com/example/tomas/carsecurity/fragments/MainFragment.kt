package com.example.tomas.carsecurity.fragments

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.content.res.AppCompatResources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.context.UtilsContext
import com.example.tomas.carsecurity.utils.UtilsEnum
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.content_main.view.*

class MainFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    enum class BroadcastKeys {
        BroadcastUpdateUI, KeyShowMessage, KeyUtilName, KeyUtilActivated
    }

    private lateinit var utilsContext: UtilsContext
    private var isProgressRun = false
    private var canShowProgress = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        utilsContext = UtilsContext(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.content_main, container, false)

        view.power_save_indication.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_battery_alert_green_24dp),
                null,
                null,
                null)

        view.actionAlarm.setOnClickListener {
            canShowProgress = true
            sendIntentSwitchUtil(UtilsEnum.Alarm)
        }

        view.actionTracker.setOnClickListener {
            sendIntentSwitchUtil(UtilsEnum.Tracker)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        canShowProgress = false

        val broadcastManager = LocalBroadcastManager.getInstance(requireContext())
        broadcastManager.registerReceiver(receiver, IntentFilter(BroadcastKeys.BroadcastUpdateUI.name))

        changeColor(actionTracker, false)
        changeColor(actionAlarm, false)


        sendIntent(MainService.Actions.ActionStatusUI.name)

        setVisibility(actionAlarm, utilsContext.isAlarmAllowed)
        setVisibility(actionTracker, utilsContext.isTrackerAllowed)
        setVisibility(power_save_indication, utilsContext.isPowerSaveMode)

        utilsContext.registerOnPreferenceChanged(this)
    }

    override fun onStop() {
        super.onStop()

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
        utilsContext.unregisterOnPreferenceChanged(this)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "BroadcastReceiver.onReceive was triggered.")

            if (intent.hasExtra(BroadcastKeys.KeyShowMessage.name)) {

                // Show alert dialog
                AlertDialog.Builder(this@MainFragment.requireActivity())
                        .setMessage(intent.getStringExtra(BroadcastKeys.KeyShowMessage.name))
                        .setTitle(R.string.error_msg_title)
                        .setPositiveButton(R.string.ok, null)
                        .create().show()

            } else {
                val utilName = intent.getStringExtra(BroadcastKeys.KeyUtilName.name)
                val utilEnabled = intent.getBooleanExtra(BroadcastKeys.KeyUtilActivated.name, false)

                when (utilName) {
                    UtilsEnum.Alarm.name -> {
                        if (utilEnabled && canShowProgress) {
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

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.key_tool_alarm_is_allowed) -> setVisibility(actionAlarm, utilsContext.isAlarmAllowed)
            getString(R.string.key_tool_tracker_is_allowed) -> setVisibility(actionTracker, utilsContext.isTrackerAllowed)
            getString(R.string.key_tool_battery_mode) -> {
                setVisibility(actionAlarm, utilsContext.isAlarmAllowed)
                setVisibility(actionTracker, utilsContext.isTrackerAllowed)
                setVisibility(power_save_indication, utilsContext.isPowerSaveMode)
            }
        }
    }

    private fun runProgress(context: Context) {
        progressBar.max = UtilsContext(context).startAlarmInterval / 1000
        progressBar.progress = 0

        if (!isProgressRun) {
            isProgressRun = true
            progressBar.visibility = ProgressBar.VISIBLE

            val handler = Handler()

            Thread(Runnable {
                while (progressBar != null && progressBar.max > progressBar.progress) {
                    handler.post {
                        progressBar.progress++
                    }

                    Thread.sleep(1000)
                }

                handler.post {
                    progressBar?.visibility = ProgressBar.GONE
                    isProgressRun = false
                }
            }).start()
        }
    }

    private fun setVisibility(button: View, visible: Boolean) {
        button.visibility = if (visible) {
            Button.VISIBLE
        } else {
            Button.GONE
        }
    }

    private fun changeColor(button: Button, enabled: Boolean) {
        if (enabled) {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.enabled))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.disabled))
        }
    }

    private fun sendIntent(action: String) {
        val applicationContext: Context = requireContext()

        val intent = Intent(applicationContext, MainService::class.java)
        intent.action = action
        applicationContext.startService(intent)
    }

    private fun sendIntentSwitchUtil(util: UtilsEnum) {
        val applicationContext: Context = requireContext()

        val intent = Intent(applicationContext, MainService::class.java)
        intent.action = MainService.Actions.ActionSwitchUtil.name
        intent.putExtra("util", util)
        applicationContext.startService(intent)
    }
}