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
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.context.ToolsContext
import com.example.tomas.carsecurity.storage.Storage
import com.example.tomas.carsecurity.tools.ToolsEnum
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.content_main.view.*

/**
 * Class represents main fragment which is displayed when application start. View contains only
 * buttons to control tools.
 */
class MainFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Broadcast keys which can be used to communicate with this class over [BroadcastReceiver].
     */
    enum class BroadcastKeys {
        BroadcastUpdateUI, KeyShowMessage, KeyUtilName, KeyUtilActivated, KeyAlarm
    }

    /** Instance of [ToolsContext]. */
    private lateinit var toolsContext: ToolsContext
    /** Instance of [CommunicationContext]. */
    private lateinit var communicationContext: CommunicationContext
    /** Indication if alarm activation progress bar is active */
    private var isProgressRun = false
    /** Indication if alarm activation progress bar can be visible */
    private var canShowProgress = false

    /**
     * Method only initialize [toolsContext] and [communicationContext]
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolsContext = ToolsContext(requireContext())
        communicationContext = CommunicationContext(requireContext())
    }

    /**
     * Method initialize view of fragment and sets buttons listeners.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.content_main, container, false)

        view.power_save_indication.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_battery_alert_green_24dp),
                null,
                null,
                null)

        view.actionAlarm.setOnClickListener {
            canShowProgress = true
            sendIntentSwitchUtil(ToolsEnum.Alarm)
        }

        view.actionTracker.setOnClickListener {
            sendIntentSwitchUtil(ToolsEnum.Tracker)
        }

        return view
    }

    /**
     * Method register this class to [BroadcastReceiver], display only allowed buttons and send request
     * to [MainService] to get information about actual application status.
     */
    override fun onResume() {
        super.onResume()
        canShowProgress = false

        val broadcastManager = LocalBroadcastManager.getInstance(requireContext())
        broadcastManager.registerReceiver(receiver, IntentFilter(BroadcastKeys.BroadcastUpdateUI.name))

        changeColor(actionTracker, false)
        changeColor(actionAlarm, false)

        sendIntent(MainService.Actions.ActionStatusUI.name)

        Thread(Runnable {
            Log.d(tag, "Check if user is login correctly.")
            val storage = Storage.getInstance(requireContext())
            val user = storage.userService.getUser()
            if (user == null || user.carId == -1L || user.username.isBlank()) {
                Log.d(tag, "Set preference is login to false. Incorrect login.")
                communicationContext.isLogin = false
                storage.clearAllTables()
            }
        }).start()

//        setVisibility(actionAlarm, toolsContext.isAlarmAllowed)
//        setVisibility(actionTracker, toolsContext.isTrackerAllowed)
        setVisibility(power_save_indication, toolsContext.isPowerSaveMode)

        toolsContext.registerOnPreferenceChanged(this)
        setTrackerLength()
    }

    /**
     * Method only unregister this class from [BroadcastReceiver].
     */
    override fun onStop() {
        super.onStop()

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
        toolsContext.unregisterOnPreferenceChanged(this)
    }

    /**
     * Broadcast receiver which handle requests to show error to user or status information about actual
     * application state.
     */
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

            } else if (intent.hasExtra(BroadcastKeys.KeyAlarm.name)) {
                actionAlarm.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.alarm))
            } else {
                val utilName = intent.getStringExtra(BroadcastKeys.KeyUtilName.name)
                val utilEnabled = intent.getBooleanExtra(BroadcastKeys.KeyUtilActivated.name, false)

                when (utilName) {
                    ToolsEnum.Alarm.name -> {
                        if (utilEnabled && canShowProgress) {
                            runProgress()
                        } else {
                            progressBar.max = 0
                        }
                        changeColor(actionAlarm, utilEnabled)
                    }
                    ToolsEnum.Tracker.name -> changeColor(actionTracker, utilEnabled)
                }
            }
        }
    }

    /**
     * Method is automatically triggered when value in shared preferences is changed. Method take action only
     * when keys:
     * tool_alarm_is_allowed
     * tool_tracker_is_allowed
     * tool_battery_mode
     *
     * @param p0 is [SharedPreferences] in which was value changed.
     * @param key is key of actual changed value.
     */
    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.key_tool_alarm_is_allowed) -> setVisibility(actionAlarm, toolsContext.isAlarmAllowed)
            getString(R.string.key_tool_tracker_is_allowed) -> setVisibility(actionTracker, toolsContext.isTrackerAllowed)
            getString(R.string.key_tool_battery_mode) -> {
//                setVisibility(actionAlarm, toolsContext.isAlarmAllowed)
//                setVisibility(actionTracker, toolsContext.isTrackerAllowed)
                setVisibility(power_save_indication, toolsContext.isPowerSaveMode)
            }
            getString(R.string.key_tool_tracker_actual_length) -> setTrackerLength()
        }
    }

    /**
     * Display progress bar to user and starts thread which changing its value.
     */
    private fun runProgress() {
        progressBar.max = toolsContext.startAlarmInterval / 1000
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

    /**
     * Method set text to tracker button. Text is set according to value from sharedPreferences
     * with key_tool_tracker_actual_length. When value is smaller than 0 default text is set.
     * When value is zero or higher Then is set value as a text.
     */
    private fun setTrackerLength() {
        val length = toolsContext.actualLength
        when {
            length < 0 -> actionTracker.setText(R.string.tracker_button_text)
            length < 500 -> actionTracker.text = requireContext().getString(
                    R.string.tracker_button_text_meters, toolsContext.actualLength)
            else -> actionTracker.text = requireContext().getString(
                    R.string.tracker_button_text_kilometers, toolsContext.actualLength / 1000)
        }
    }

    /**
     * Method sets given visibility to button specified with [button] value.
     *
     * @param button is view of which visibility will be changed.
     * @param visible true = VISIBLE, false = GONE
     */
    private fun setVisibility(button: View, visible: Boolean) {
        button.visibility = if (visible) {
            Button.VISIBLE
        } else {
            Button.GONE
        }
    }

    /**
     * Method change color of [button]. Color is specified by [enabled].
     *
     * @param button is button of which value will be changed.
     * @param enabled true = Green color, false = Red color.
     */
    private fun changeColor(button: Button, enabled: Boolean) {
        if (enabled) {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.enabled))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.disabled))
        }
    }

    /**
     * Method send direct message over Intent to [MainService]. Intent action is specified by [action].
     *
     * @param action is action which will be send to [MainService].
     */
    private fun sendIntent(action: String) {
        val applicationContext: Context = requireContext()

        val intent = Intent(applicationContext, MainService::class.java)
        intent.action = action
        applicationContext.startService(intent)
    }

    /**
     * Method send direct message over Intent to [MainService]. Message sends command to change actual state of [util].
     *
     * @param util specification of util of which status should be changed.
     */
    private fun sendIntentSwitchUtil(util: ToolsEnum) {
        val applicationContext: Context = requireContext()

        val intent = Intent(applicationContext, MainService::class.java)
        intent.action = MainService.Actions.ActionSwitchUtil.name
        intent.putExtra("util", util)
        applicationContext.startService(intent)
    }
}