package com.example.tomas.carsecurity.communication.network

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.storage.Storage
import com.example.tomas.carsecurity.utils.UtilsEnum
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseService : FirebaseMessagingService() {

    private val tag = "FirebaseService"

    enum class Commands {
        Status, Activate, Deactivate
    }

    fun updateFirebaseToken(context: Context) {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
            storeToken(instanceIdResult?.token, context)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val user = Storage.getInstance(this).userService.getUser()
        if (user == null){
            Log.d(tag, "Can not response on Firebase message. User is not login")
            return
        }

        if (user.username != remoteMessage.data["username"]) {
            Log.d(tag, "Can not response on Firebase message. Request from invalid user.")
            return
        }

        val commandRaw = remoteMessage.data["command"]
        if (commandRaw == null || commandRaw.isBlank()){
            Log.w(tag, "Incoming Firebase message has no command.")
            return
        }

        try {
            Log.d(tag, "Firebase command: $commandRaw")
            val command = Commands.valueOf(commandRaw)
            when (command) {
                Commands.Status -> sendIntentStatus()
                Commands.Activate -> switchUtil(remoteMessage.data["tool"], true)
                Commands.Deactivate -> switchUtil(remoteMessage.data["tool"], false)
            }
        } catch (e: IllegalArgumentException){
            Log.w(tag, "Can not get data from incoming message")
        }
    }

    override fun onNewToken(token: String?) {
        Log.d(tag, "New Firebase refresh token received.")

        if (this.applicationContext == null) {
            Log.d(tag, "Can not store token. Context is not defined.")
            return
        }

        storeToken(token, applicationContext)
    }

    private fun storeToken(token: String?, context: Context){

        if (token == null || token.isBlank()) {
            Log.d(tag, "New Firebase token is null or blank")
            return
        }

        val sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        sharedPreferences
                .edit()
                .putString(context.getString(R.string.key_communication_network_firebase_token), token)
                .apply()
    }

    private fun switchUtil(tool: String?, activate: Boolean){

        if (tool == null || tool.isBlank()) throw IllegalArgumentException()
        val utilEnum = UtilsEnum.valueOf(tool)

        val intent = Intent(this, MainService::class.java)
        intent.action = if(activate) MainService.Actions.ActionActivateUtil.name
                        else MainService.Actions.ActionDeactivateUtil.name
        intent.putExtra("util", utilEnum)
        this.startService(intent)

    }

    private fun sendIntentStatus(){
        Log.d(tag, "Sending intent to send status message over network.")

        val intent = Intent(this, MainService::class.java)
        intent.action = MainService.Actions.ActionStatus.name
        intent.putExtra("communicator", NetworkProvider::class.hashCode())
        this.startService(intent)
    }
}
