package com.example.tomas.carsecurity

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.Alarm
import com.example.tomas.carsecurity.utils.UtilsManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    private val deviceAdr:String = "00:11:67:63:45:CE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val context = MyContext(applicationContext)

        val utilsManager = UtilsManager(context)
        val alarm = Alarm(context, utilsManager)

        actionAlarm.setOnClickListener { if(alarm.isEnabled()) alarm.disableArarm() else alarm.enableAlarm() }

        actionForeground.setOnClickListener {
            val intent = Intent(applicationContext, MainService::class.java)
            intent.action = MainService.Actions.action_start.name
            startService(intent)
        }

        actionForegroundStop.setOnClickListener {
            val intent = Intent(applicationContext, MainService::class.java)
            intent.action = MainService.Actions.action_stop.name
            startService(intent)
        }
    }



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
