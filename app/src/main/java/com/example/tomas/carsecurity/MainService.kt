package com.example.tomas.carsecurity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.Alarm
import com.example.tomas.carsecurity.utils.GeneralUtil
import com.example.tomas.carsecurity.utils.UtilsManager
import java.util.concurrent.atomic.AtomicInteger

class MainService : Service() {

    enum class Actions{
        ActionTryStopService, ActionStopService, ActionAlarm, ActionTracker, ActionWifiHotSpot, ActionAutomaticMode;

        fun getInstance(context: MyContext, utilsManager: UtilsManager): GeneralUtil{
            return when (this){
                ActionAlarm -> Alarm(context, utilsManager)
                else -> throw UnsupportedOperationException("Class not implemented")
            }
        }
    }

    private val workerThread = WorkerThread("MainServiceThread")
    private lateinit var context: MyContext
    private lateinit var utilsManager: UtilsManager

    private var tasksInQueue :AtomicInteger = AtomicInteger(0)

    private val utilsMap: MutableMap<Actions, GeneralUtil> = HashMap()

    override fun onCreate() {
        super.onCreate()

        println("MainService: On create was called") // TODO log

        workerThread.start()
        workerThread.prepareHandler()
        startForeground()
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO workerThread.quit() ??
        // TODO stopForeground(true)?? stopSelf()??
        // TODO what to do when system kill service on low memory
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (! ::context.isInitialized) context = MyContext(applicationContext)
        if (! ::utilsManager.isInitialized) utilsManager = UtilsManager(context)

        if(intent != null){
            val action: String = intent.action

            when(action){
                Actions.ActionAlarm.name -> switchUtil(Actions.ActionAlarm)
                Actions.ActionStopService.name -> stopService()
                Actions.ActionTryStopService.name ->
                    if(tasksInQueue.get() == 0 && !utilsManager.isAnyUtilEnabled()) stopService()


//                Actions.ActionStop.name -> {
//                    workerThread.quit()
//                    stopForeground(true)
//                    stopSelf()
//                }

                else -> println("empty action")
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun switchUtil(action: Actions){
        val task = Runnable {
            // task run sequentially in one thread
            val util: GeneralUtil = utilsMap[action] ?: action.getInstance(context, utilsManager)

            if (util.isEnabled()) {
                util.disable()
            } else {
                util.enable()
            }

            utilsMap[action] = util
            tasksInQueue.decrementAndGet()
        }
        tasksInQueue.incrementAndGet()
        workerThread.postTask(task)
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
