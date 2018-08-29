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
import com.example.tomas.carsecurity.utils.UtilsEnum
import com.example.tomas.carsecurity.utils.UtilsManager
import java.util.concurrent.atomic.AtomicInteger

class MainService : Service(){

    enum class Actions{
        ActionTryStopService, ActionStopService, ActionStatus, ActionStatusUI, ActionGetPosition,
        ActionSwitchUtil, ActionActivateUtil, ActionDeactivateUtil, ActionAutomaticMode;
    }

    private val tag = "MainService"
    private val notificationId = 973920
    private val workerThread = WorkerThread("MainServiceThread")

    private lateinit var broadcastSender: BroadcastSender
    private lateinit var utilsManager: UtilsManager
    private lateinit var context: MyContext

    private var tasksInQueue :AtomicInteger = AtomicInteger(0)


    override fun onDestroy() {
        super.onDestroy()
        workerThread.quit()
        utilsManager.destroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (! ::context.isInitialized) context = MyContext(applicationContext)
        if (! ::broadcastSender.isInitialized) broadcastSender = BroadcastSender(applicationContext)
        // intent is null when application is restarted when system kill service
        if (! ::utilsManager.isInitialized) utilsManager = UtilsManager(context, broadcastSender, intent == null)

        if(intent != null){
            when(intent.action){
                Actions.ActionStopService.name -> stopService(false)
                Actions.ActionTryStopService.name -> stopService(true)
                Actions.ActionSwitchUtil.name -> switchUtil(intent.getSerializableExtra("util") as UtilsEnum, Actions.ActionSwitchUtil)
                Actions.ActionActivateUtil.name -> switchUtil(intent.getSerializableExtra("util") as UtilsEnum, Actions.ActionActivateUtil)
                Actions.ActionDeactivateUtil.name -> switchUtil(intent.getSerializableExtra("util") as UtilsEnum, Actions.ActionDeactivateUtil)

                Actions.ActionStatusUI.name -> broadcastSender.informUI(utilsManager.getEnabledUtils())
                else -> Log.d(tag, "onStartCommand - invalid action")
            }
        }

        return Service.START_STICKY
    }

    private fun switchUtil(utilEnum: UtilsEnum, actions: Actions){
        if(!workerThread.isAlive){
            Log.d(tag, "Start foreground service")
            workerThread.start()
            workerThread.prepareHandler()
            startForeground()
        }

        val task = Runnable {
            // task run sequentially in one thread
            val result = when(actions){
                Actions.ActionSwitchUtil -> utilsManager.switchUtil(utilEnum)
                Actions.ActionActivateUtil -> utilsManager.activateUtil(utilEnum)
                Actions.ActionDeactivateUtil -> utilsManager.deactivateUtil(utilEnum)
                else -> {true}
            }

            tasksInQueue.decrementAndGet()
            if(!result){
                stopService(true)
            }
        }
        tasksInQueue.incrementAndGet()
        workerThread.postTask(task)
    }

    private fun stopService(safely: Boolean){
        if(!safely || (tasksInQueue.get() == 0 && !utilsManager.isAnyUtilEnabled())){
            stopForeground(true)
            stopSelf()
        }
    }

    private fun startForeground() {
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
