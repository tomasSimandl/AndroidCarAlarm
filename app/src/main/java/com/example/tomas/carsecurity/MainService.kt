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
        ActionStatus, ActionStatusUI, ActionGetPosition,
        ActionSwitchUtil, ActionActivateUtil, ActionDeactivateUtil, ActionAutomaticMode;
    }

    private val tag = "MainService"
    private val notificationId = 973920 // random number

    private lateinit var workerThread: WorkerThread
    private lateinit var broadcastSender: BroadcastSender
    private lateinit var utilsManager: UtilsManager
    private lateinit var context: MyContext

    private var isForeground = false
    private var tasksInQueue :AtomicInteger = AtomicInteger(0)


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
        if (! ::utilsManager.isInitialized) utilsManager = UtilsManager(context, broadcastSender, intent == null)


        if(intent != null){


            val task = Runnable {
                // can be there because tasks run sequentially in one thread and when I
                // call stopService i need to be counter without this thread.
                tasksInQueue.decrementAndGet()

                val activation = when(intent.action) {
                    Actions.ActionSwitchUtil.name -> utilsManager.switchUtil(intent.getSerializableExtra("util") as UtilsEnum)
                    Actions.ActionActivateUtil.name -> utilsManager.activateUtil(intent.getSerializableExtra("util") as UtilsEnum)
                    Actions.ActionDeactivateUtil.name -> utilsManager.deactivateUtil(intent.getSerializableExtra("util") as UtilsEnum)
                    else -> null
                }

                when(activation) {
                    true -> startForeground()
                    false -> stopService(true)
                    null -> {
                        // process other actions
                        when(intent.action) {

                            Actions.ActionStatusUI.name -> broadcastSender.informUI(utilsManager.getEnabledUtils())
                            else -> Log.w(tag, "onStartCommand - invalid action")
                        }
                    }
                }
            }

            tasksInQueue.incrementAndGet()
            workerThread.postTask(task)
        }

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        workerThread.quit()
        utilsManager.destroy()
    }

    private fun stopService(safely: Boolean){
        if(!safely || (tasksInQueue.get() == 0 && !utilsManager.isAnyUtilEnabled())){
            stopForeground(true)
            stopSelf()
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
