package com.example.tomas.carsecurity.communication.network

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.context.CommunicationContext
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.Serializable

class FirebaseService : FirebaseMessagingService() {

    private val tag = "FirebaseService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // ...



        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(tag, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(tag, "Message data payload: ${remoteMessage.data}")

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
//                scheduleJob()
            } else {
                // Handle message within 10 seconds
//                handleNow()
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            Log.d(tag, "Message Notification Body: ${remoteMessage.notification!!.body.toString()}")
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.



        sendIntentStatus()
    }

    override fun onNewToken(token: String) {
        Log.d(tag, "New Firebase refresh token received.")

        val sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        sharedPreferences
                .edit()
                .putString(getString(R.string.key_communication_network_firebase_token), token)
                .apply()
    }

    private fun sendIntentStatus(){
        Log.d(tag, "Sending intent to send status message over network.")

        val intent = Intent(this, MainService::class.java)
        intent.action = MainService.Actions.ActionStatus.name
        intent.putExtra("communicator", NetworkProvider::class.hashCode())
        this.startService(intent)
    }
}
