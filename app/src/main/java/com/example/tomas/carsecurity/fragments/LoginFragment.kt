package com.example.tomas.carsecurity.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.HandlerThread
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.CommunicationManager
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.storage.Storage
import com.example.tomas.carsecurity.utils.UtilsEnum
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.login_fragment.*
import kotlinx.android.synthetic.main.login_fragment.view.*

class LoginFragment : Fragment() {

    enum class BroadcastKeys {
        BroadcastLoginResult, KeySuccess, KeyErrorMessage
    }

    private lateinit var communicationManager: CommunicationManager


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.login_fragment, container, false)
        view.btn_login.setOnClickListener { loginButtonAction() }
        view.btn_logout.setOnClickListener { logoutButtonAction() }

        return view
    }

    override fun onResume() {
        super.onResume()

        communicationManager = CommunicationManager(CommunicationContext(requireContext()))

        login_error_text_view.visibility = View.GONE

        Thread(Runnable {
            val user = Storage.getInstance(requireContext()).userService.getUser()

            if (user == null) {
                showLogin()
            } else {
                showLogout(user.username)
            }
        }).start()

        val broadcastManager = LocalBroadcastManager.getInstance(requireContext())
        broadcastManager.registerReceiver(receiver, IntentFilter(BroadcastKeys.BroadcastLoginResult.name))
    }

    override fun onStop() {
        super.onStop()

        communicationManager.destroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "BroadcastReceiver.onReceive was triggered.")

            input_password.text.clear()

            if (intent.getBooleanExtra(BroadcastKeys.KeySuccess.name, false)) {
                showLogout(input_username.text.toString())
            } else {
                login_error_text_view.visibility = View.VISIBLE
                login_error_text_view.text = intent.getStringExtra(BroadcastKeys.KeyErrorMessage.name)
            }

            btn_login.isEnabled = true
        }
    }

    private fun showLogin(){
        logout_view.post { logout_view.visibility = View.GONE }
        login_view.post { login_view.visibility = View.VISIBLE }
    }

    private fun showLogout(username: String) {
        logout_view.post { logout_view.visibility = View.VISIBLE }
        login_view.post { login_view.visibility = View.GONE }
        login_user_text_view.post {
            login_user_text_view.text = requireContext().getString(R.string.login_user_text, username)
        }
    }

    private fun loginButtonAction() {

        btn_login.isEnabled = false
        login_error_text_view.visibility = View.GONE

        if (!validInputs()) {
            login_error_text_view.text = requireContext().getText(R.string.err_login_invalid_inputs)
            login_error_text_view.visibility = View.VISIBLE
            btn_login.isEnabled = true
            return
        }

        // show spinner
        communicationManager.sendNetworkLogin(input_username.text.toString(), input_password.text.toString())
    }

    private fun logoutButtonAction() {
        btn_logout.isEnabled = false

        Thread(Runnable {
            val storageUserService = Storage.getInstance(requireContext()).userService
            val user = storageUserService.getUser()

            if (user != null) {
                storageUserService.deleteUser(user)
            }

            btn_logout.post { btn_logout.isEnabled = true }
            showLogin()
        }).start()
    }

    private fun validInputs(): Boolean {
        return !input_username.text.isBlank() && !input_password.text.isBlank()
    }
}