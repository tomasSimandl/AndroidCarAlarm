package com.example.tomas.carsecurity.xxfunctionalityTests

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class YourBroadcastReciver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        println("CONNECTED ++++++++++++++++++++++++++++++++++++++++")



        val action:String = intent.action
        if (BluetoothDevice.ACTION_FOUND == action) {
            // A Bluetooth device was found
            // Getting device information from the intent
            var device:BluetoothDevice  = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            println("Device found: " + device.getName() + "; MAC " + device.getAddress())
        }
    }
}
