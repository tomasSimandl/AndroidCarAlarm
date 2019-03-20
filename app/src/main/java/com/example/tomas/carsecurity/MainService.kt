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
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.tools.UtilsEnum
import com.example.tomas.carsecurity.tools.UtilsManager
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class MainService : Service(), Observer {

    enum class Actions{
        ActionStatus, ActionStatusUI, ActionGetPosition, ActionForegroundStop,
        ActionSwitchUtil, ActionActivateUtil, ActionDeactivateUtil, ActionAutomaticMode,
        ActionTryStop;
    }

    private val tag = "MainService"
    private val notificationId = 973920 // random number

    private lateinit var workerThread: WorkerThread
    private lateinit var broadcastSender: BroadcastSender
    private lateinit var utilsManager: UtilsManager
    private lateinit var context: MyContext

    private var isForeground = false
    private var tasksInQueue :AtomicInteger = AtomicInteger(0)


    override fun update(observable: Observable, args: Any) {
        if(observable is UtilsManager) {
            when(args){
                is Pair<*, *> ->
                    if(args.first is UtilsEnum && args.second is Boolean){
                        broadcastSender.informUI(args.first as UtilsEnum, args.second as Boolean)
                        if(args.second == true){
                            startForeground()
                        } else {
                            tryStopForeground()
                        }
                    }
                is Actions -> processAction(Intent(args.name))
                is String -> broadcastSender.showMessage(args)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if(! ::workerThread.isInitialized){
            workerThread = WorkerThread("MainServiceThread")
            workerThread.start()
            workerThread.prepareHandler()
        }

        if (! ::context.isInitialized) context = MyContext(applicationContext, workerThread.looper)
        if (! ::broadcastSender.isInitialized) broadcastSender = BroadcastSender(applicationContext)
        // intent is null when application is restarted when system kill service
        if (! ::utilsManager.isInitialized) {
            utilsManager = UtilsManager(context, intent == null)
            utilsManager.addObserver(this)
        }

        if(intent != null){
            processAction(intent)
        }

        return Service.START_STICKY
    }

    private fun processAction(intent: Intent){
        val task = Runnable {
            // can be there because tasks run sequentially in one thread and when I
            // call stopService i need to be counter without this thread.
            tasksInQueue.decrementAndGet()

            when(intent.action) {
                Actions.ActionSwitchUtil.name -> utilsManager.switchUtil(intent.getSerializableExtra("util") as UtilsEnum)
                Actions.ActionActivateUtil.name -> utilsManager.activateUtil(intent.getSerializableExtra("util") as UtilsEnum)
                Actions.ActionDeactivateUtil.name -> utilsManager.deactivateUtil(intent.getSerializableExtra("util") as UtilsEnum)
                Actions.ActionStatusUI.name -> broadcastSender.informUI(utilsManager.getEnabledUtils())
                Actions.ActionForegroundStop.name -> stopService(true)
                Actions.ActionTryStop.name -> if(!isForeground) stopSelf()
                Actions.ActionStatus.name -> utilsManager.sendStatus(intent.getIntExtra("communicator", -1))
                else -> Log.w(tag, "onStartCommand - invalid action")
            }
        }

        tasksInQueue.incrementAndGet()
        workerThread.postTask(task)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "Destroying")
        if (::context.isInitialized) context.destroy()
        if (::workerThread.isInitialized) workerThread.quit()
        if (::utilsManager.isInitialized) utilsManager.destroy()
    }

    private fun tryStopForeground() {
        if (tasksInQueue.get() == 0 && !utilsManager.isAnyUtilEnabled()) {
            stopForeground(true)
            isForeground = false
        }
    }

    private fun stopService(safely: Boolean){
        if(!safely || (tasksInQueue.get() == 0 && !utilsManager.isAnyUtilEnabled())){
            stopForeground(true)
            isForeground = false
        }
    }

    private fun startForeground() {
        if(isForeground) return

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
