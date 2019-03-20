package com.example.tomas.carsecurity.communication.network

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.storage.Storage
import com.example.tomas.carsecurity.tools.ToolsEnum
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseService : FirebaseMessagingService() {

    /**
     * Enum contains all possible commands which can be received over Firebase Cloud messaging.
     */
    enum class Commands {
        Status, Activate, Deactivate
    }

    /** Logger tag */
    private val tag = "FirebaseService"

    /**
     *  Method load Firebase token from Firebase Instance and call storeToken method which save
     *  token to SharedPreferences.
     *
     *  @param context application context
     */
    fun updateFirebaseToken(context: Context) {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
            storeToken(instanceIdResult?.token, context)
        }
    }

    /**
     * Method is triggered when message is received over Firebase Cloud messaging.
     * Method call adequate method which is given by command from input message. Command is
     * specified by Commands enum. For success user must be login and username must be equal
     * to username which is defined in message.
     *
     * @param remoteMessage incoming message which must contains username of login user and command.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val user = Storage.getInstance(this).userService.getUser()
        if (user == null) {
            Log.d(tag, "Can not response on Firebase message. User is not login")
            return
        }

        if (user.username != remoteMessage.data["username"]) {
            Log.d(tag, "Can not response on Firebase message. Request from invalid user.")
            return
        }

        val commandRaw = remoteMessage.data["command"]
        if (commandRaw == null || commandRaw.isBlank()) {
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
        } catch (e: IllegalArgumentException) {
            Log.w(tag, "Can not get data from incoming message")
        }
    }

    /**
     * Method is call when new Firebase token is created.
     * Warning: When user login to application new token is not created.
     *
     * Token is store to SharedPreferences over method storeToken
     *
     * @param token new Firebase token.
     */
    override fun onNewToken(token: String?) {
        Log.d(tag, "New Firebase refresh token received.")

        if (this.applicationContext == null) {
            Log.d(tag, "Can not store token. Context is not defined.")
            return
        }

        storeToken(token, applicationContext)
    }

    /**
     * Method store input token to SharedPreferences. Token should not be null or blank.
     *
     * @param token Firebase token which will be stored to SharedPreferences.
     * @param context application context used to open SharedPreferences.
     */
    private fun storeToken(token: String?, context: Context) {

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

    /**
     * Method is used for Commands.Activate and Commands.Deactivate. Method send command directly
     * to MainService which can handle this request.
     *
     * @param tool name of tool which should be activated/deactivated
     * @param activate true - activate, false - deactivate
     */
    private fun switchUtil(tool: String?, activate: Boolean) {

        if (tool == null || tool.isBlank()) throw IllegalArgumentException()
        val utilEnum = ToolsEnum.valueOf(tool)

        val intent = Intent(this, MainService::class.java)
        intent.action = if (activate) MainService.Actions.ActionActivateUtil.name
        else MainService.Actions.ActionDeactivateUtil.name
        intent.putExtra("util", utilEnum)
        this.startService(intent)

    }

    /**
     * Method is used for Commands.Status. Method send status request directly to MainService.
     */
    private fun sendIntentStatus() {
        Log.d(tag, "Sending intent to send status message over network.")

        val intent = Intent(this, MainService::class.java)
        intent.action = MainService.Actions.ActionStatus.name
        intent.putExtra("communicator", NetworkProvider::class.hashCode())
        this.startService(intent)
    }
}
