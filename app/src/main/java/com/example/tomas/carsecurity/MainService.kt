package com.example.tomas.carsecurity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.example.tomas.carsecurity.activities.MainActivity
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.tools.ToolsEnum
import com.example.tomas.carsecurity.tools.ToolsManager
import com.example.tomas.carsecurity.utils.UIBroadcastsSender
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Service which process all users commands and represents main logic of application.
 */
class MainService : Service(), Observer {

    /**
     * Enum of actions which can [MainService] accept.
     */
    enum class Actions {
        ActionStatus, ActionStatusUI, ActionGetPosition, ActionForegroundStop,
        ActionSwitchUtil, ActionActivateUtil, ActionDeactivateUtil, ActionAutomaticMode,
        ActionTryStop;
    }

    /** Logger tag */
    private val tag = "MainService"
    /** Random number for foreground notification */
    private val notificationId = 973920 // random number

    /** Thread for running tasks on MainServiceThread */
    private lateinit var workerThread: WorkerThread
    /** Class used for sending broadcast messages to MainFragment */
    private lateinit var UIBroadcastsSender: UIBroadcastsSender
    /** Manager for manage tools */
    private lateinit var toolsManager: ToolsManager
    /** Context used for accessing values in sharedPreferences */
    private lateinit var context: MyContext

    /** Indication if class service is in Foreground */
    private var isForeground = false

    /** Counter of actual commands in queue */
    private var tasksInQueue: AtomicInteger = AtomicInteger(0)

    /**
     * Method called when observer class call notifyObservers method. Method process notifications
     * from tools and sensors.
     *
     * Only accepted [observable] is [ToolsManager]
     * Method inform UI or take same action according to input [args].
     *
     * @param observable which call notifyObservers method
     * @param args aditional parameters.
     */
    override fun update(observable: Observable, args: Any) {
        if (observable is ToolsManager) {
            when (args) {
                is Pair<*, *> ->
                    if (args.first is ToolsEnum && args.second is Boolean) {
                        UIBroadcastsSender.informUI(args.first as ToolsEnum, args.second as Boolean)
                        if (args.second == true) {
                            startForeground()
                        } else {
                            tryStopForeground()
                        }
                    }
                is Actions -> processAction(Intent(args.name))
                is String -> UIBroadcastsSender.showMessage(args)
            }
        }
    }

    /**
     * Method is called when service is bind to any other component.
     * Method is not implemented and cause exception [UnsupportedOperationException]
     */
    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not implemented")
    }

    /**
     * Access point to service. On first call method initialize service.
     * Method call [processAction] method to process input intent.
     *
     * @param intent which contains data about requested action.
     * @param flags not used
     * @param startId not used
     * @return START STICKY
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (!::workerThread.isInitialized) {
            workerThread = WorkerThread("MainServiceThread")
            workerThread.start()
            workerThread.prepareHandler()
        }

        if (!::context.isInitialized) context = MyContext(applicationContext, workerThread.looper)
        if (!::UIBroadcastsSender.isInitialized) UIBroadcastsSender = UIBroadcastsSender(applicationContext)
        // intent is null when application is restarted when system kill service
        if (!::toolsManager.isInitialized) {
            toolsManager = ToolsManager(context)
            toolsManager.addObserver(this)
        }

        if (intent != null) {
            processAction(intent)
        }

        return Service.START_STICKY
    }

    /**
     * Method process input intent. Intent.action must contains action from [Actions] enum.
     * Next must contains data specified by given action.
     * Processing run in MainServiceThread thread.
     *
     * @param intent which contains specification of required action.
     */
    private fun processAction(intent: Intent) {
        val task = Runnable {
            // can be there because tasks run sequentially in one thread and when I
            // call stopService i need to be counter without this thread.
            tasksInQueue.decrementAndGet()

            when (intent.action) {
                Actions.ActionSwitchUtil.name -> toolsManager.switchUtil(intent.getSerializableExtra("util") as ToolsEnum)
                Actions.ActionActivateUtil.name -> toolsManager.activateUtil(intent.getSerializableExtra("util") as ToolsEnum)
                Actions.ActionDeactivateUtil.name -> toolsManager.deactivateUtil(intent.getSerializableExtra("util") as ToolsEnum)
                Actions.ActionStatusUI.name -> UIBroadcastsSender.informUI(toolsManager.getEnabledUtils())
                Actions.ActionForegroundStop.name -> stopService(true)
                Actions.ActionTryStop.name -> if (!isForeground) stopSelf()
                Actions.ActionStatus.name -> toolsManager.sendStatus(intent.getIntExtra("communicator", -1))
                else -> Log.w(tag, "onStartCommand - invalid action")
            }
        }

        tasksInQueue.incrementAndGet()
        workerThread.postTask(task)
    }

    /**
     * Method destroy all initialized data and deinitialize all tool and sensors.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "Destroying")
        if (::context.isInitialized) context.destroy()
        if (::workerThread.isInitialized) workerThread.quit()
        if (::toolsManager.isInitialized) toolsManager.destroy()
    }

    /**
     * Method turn of foreground mode of service only when there is no enabled tool.
     */
    private fun tryStopForeground() {
        if (tasksInQueue.get() == 0 && !toolsManager.isAnyUtilEnabled()) {
            stopForeground(true)
            isForeground = false
        }
    }

    /**
     * Method turn of foreground mode of service.
     */
    private fun stopService(safely: Boolean) {
        if (!safely || (tasksInQueue.get() == 0 && !toolsManager.isAnyUtilEnabled())) {
            stopForeground(true)
            isForeground = false
        }
    }

    /**
     * Method enable foreground mode of this service and show notification to user.
     */
    private fun startForeground() {
        if (isForeground) return

        Log.d(tag, "Starting foreground service")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val pendingIntent = PendingIntent.getActivity(
                this.applicationContext,
                0,
                Intent(this.applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this.applicationContext, getString(R.string.chanel_id))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(this.getString(R.string.app_name))
                .setContentText(this.getString(R.string.notification_description))
                .setContentIntent(pendingIntent)
                .build()

        startForeground(notificationId, notification)
        isForeground = true
    }

    /**
     * Method create and apply notification chanel for foreground service.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                    getString(R.string.chanel_id),
                    getString(R.string.foreground_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT)

            channel.description = getString(R.string.foreground_channel_description)

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
