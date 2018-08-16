package com.example.tomas.carsecurity

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.utils.Alarm
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.sensors.MoveDetector
import com.example.tomas.carsecurity.sensors.SoundDetector
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    private val deviceAdr:String = "00:11:67:63:45:CE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val context: MyContext = MyContext(applicationContext)

        val soundDetector: GeneralObservable = SoundDetector(context)
        val moveDetector: GeneralObservable = MoveDetector(context)
        val alarmListener = Alarm(context, console)
        val locationService = LocationProvider(applicationContext)

        actionRegMoveDetector.setOnClickListener { moveDetector.addObserver(alarmListener) }
        actionRegSoundDetector.setOnClickListener { soundDetector.addObserver(alarmListener) }
        actionUnregMoveDetector.setOnClickListener { moveDetector.deleteObserver(alarmListener) }
        actionUnregSoundDetector.setOnClickListener { soundDetector.deleteObserver(alarmListener) }

        locationService.addObserver(alarmListener)
        actionEnableGps.setOnClickListener { locationService.enable() }
        actionDisableGps.setOnClickListener { locationService.disable() }

        actionStatus.setOnClickListener {
            console.text = ""
            console.append("""Move detector enabled: ${moveDetector.isEnable()}""")
            console.append("\n")
            console.append("""Sound detector enabled: ${soundDetector.isEnable()}""")
            console.append("\n")
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
