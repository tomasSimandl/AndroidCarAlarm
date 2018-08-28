package com.example.tomas.carsecurity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class MainService : Service(){

    enum class Actions{
        ActionTryStopService, ActionStopService, ActionStatus, ActionStatusUI, ActionGetPosition,
        ActionSwitchUtil, ActionActivateUtil, ActionDeactivateUtil, ActionAutomaticMode;
    }

    private val tag = "MainService"
    private val workerThread = WorkerThread("MainServiceThread")
    private lateinit var broadcastSender: BroadcastSender
    private lateinit var context: MyContext
    private lateinit var utilsManager: UtilsManager

    private var tasksInQueue :AtomicInteger = AtomicInteger(0)

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "On create was called")
    }

    override fun onDestroy() {
        super.onDestroy()
        workerThread.quit()
        // TODO stopForeground(true)?? stopSelf()??
        // TODO what to do when system kill service on low memory
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (! ::context.isInitialized) context = MyContext(applicationContext)
        if (! ::broadcastSender.isInitialized) broadcastSender = BroadcastSender(applicationContext)
        if (! ::utilsManager.isInitialized) utilsManager = UtilsManager(context, broadcastSender)

        if(intent != null){
            when(intent.action){
                Actions.ActionSwitchUtil.name -> switchUtil(intent.getSerializableExtra("util") as UtilsEnum, Actions.ActionSwitchUtil)
                Actions.ActionActivateUtil.name -> switchUtil(intent.getSerializableExtra("util") as UtilsEnum, Actions.ActionActivateUtil)
                Actions.ActionDeactivateUtil.name -> switchUtil(intent.getSerializableExtra("util") as UtilsEnum, Actions.ActionDeactivateUtil)
                Actions.ActionStopService.name -> stopService()
                Actions.ActionTryStopService.name -> stopServiceSafely()
                Actions.ActionStatusUI.name -> broadcastSender.informUI(utilsManager.getEnabledUtils())


//                Actions.ActionStop.name -> {
//                    workerThread.quit()
//                    stopForeground(true)
//                    stopSelf()
//                }

                else -> Log.d(tag, "empty action")
            }
        }

        return super.onStartCommand(intent, flags, startId)
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
                stopServiceSafely()
            }

        }
        tasksInQueue.incrementAndGet()
        workerThread.postTask(task)
    }



    private fun stopServiceSafely(){
        if(tasksInQueue.get() == 0 && !utilsManager.isAnyUtilEnabled())
            stopService()
    }

    private fun stopService(){
        workerThread.quit()
        stopForeground(true)
        stopSelf()
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val notification = NotificationCompat.Builder(this, getString(R.string.chanel_id))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Car Security")
                .setContentText("Car security is running in background")
                .build()

        startForeground(101, notification) // TODO magic constant
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
