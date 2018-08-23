package com.example.tomas.carsecurity

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.tomas.carsecurity.utils.Alarm
import kotlinx.android.synthetic.main.activity_main.*
import android.support.v4.content.LocalBroadcastManager
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Button
import com.example.tomas.carsecurity.utils.Tracker


class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "BroadcastReceiver.onReceive was triggered.")
            val utilName = intent.getStringExtra(getString(R.string.key_util_name))
            val utilEnabled = intent.getBooleanExtra(getString(R.string.key_util_activated), false)

            console.text = "Received broadcast from: \n" + utilName + "\nwith request: " + utilEnabled + "\n" + console.text

            when (utilName) {
                Alarm::class.java.canonicalName -> changeColor(actionAlarm, utilEnabled)
                Tracker::class.java.canonicalName -> changeColor(actionTracker, utilEnabled)
            }


            if(!utilEnabled){
                sendIntent(MainService.Actions.ActionTryStopService.name)
                Log.d(tag, "TryStopService intent was sent to Foreground service")
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        actionAlarm.setOnClickListener {
            sendIntent(MainService.Actions.ActionAlarm.name)
        }

        actionTracker.setOnClickListener{
            sendIntent(MainService.Actions.ActionTracker.name)
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                IntentFilter(getString(R.string.utils_ui_update))
        )
    }

    override fun onResume() {
        super.onResume()
        sendIntent(MainService.Actions.ActionStatus.name)
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onStop()
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

    private val deviceAdr:String = "00:11:67:63:45:CE"
    private fun bluetoothButtonAction(){
        val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(!adapter.isEnabled) adapter.enable()

        console.append("""size: ${adapter.bondedDevices.size}""")
        console.append("\n")
        println(adapter.bondedDevices.size)

        var device: BluetoothDevice = adapter.getRemoteDevice("00:1F:20:A8:D1:3F")


        adapter.bondedDevices.forEach {

            console.append("""$it""")
            console.append("\n")
            println(it)

            if (it.address == deviceAdr) {
                device = it
                console.append("TADA\n")
                println("TADA")
            }
        }

        adapter.startDiscovery()

//        if(device != null){
//
//
//            device!!.createRfcommSocketToServiceRecord(UUID.randomUUID())
//
//
//            println(device!!.createBond())
//
//            console.append("\n")
//
//        }


    }
}
