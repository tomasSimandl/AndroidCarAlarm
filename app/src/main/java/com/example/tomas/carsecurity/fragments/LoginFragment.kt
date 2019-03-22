package com.example.tomas.carsecurity.fragments

import android.content.*
import android.os.Bundle
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
import com.google.gson.internal.LinkedTreeMap
import kotlinx.android.synthetic.main.dialog_create_car.*
import kotlinx.android.synthetic.main.login_fragment.*
import kotlinx.android.synthetic.main.login_fragment.view.*

class LoginFragment : Fragment() {

    enum class BroadcastKeys {
        BroadcastLoginResult, BroadcastGetCarsResult, BroadcastCreateCarsResult, KeySuccess, KeyErrorMessage, KeyCars
    }

    private lateinit var communicationManager: CommunicationManager
    private lateinit var communicationContext: CommunicationContext


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.login_fragment, container, false)
        view.btn_login.setOnClickListener { loginButtonAction() }
        view.btn_logout.setOnClickListener { logoutButtonAction() }

        return view
    }

    override fun onResume() {
        super.onResume()

        if(!::communicationContext.isInitialized) communicationContext = CommunicationContext(requireContext())
        communicationManager = CommunicationManager.getInstance(communicationContext)

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
        broadcastManager.registerReceiver(getCarsReceiver, IntentFilter(BroadcastKeys.BroadcastGetCarsResult.name))
        broadcastManager.registerReceiver(createCarReceiver, IntentFilter(BroadcastKeys.BroadcastCreateCarsResult.name))
    }

    override fun onStop() {
        super.onStop()

        // communicationManager.destroy() CommunicationManager is destroyed at MainService
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(getCarsReceiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(createCarReceiver)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "BroadcastReceiver.onReceive was triggered.")

            input_password.text.clear()

            if (intent.getBooleanExtra(BroadcastKeys.KeySuccess.name, false)) {
                communicationContext.isLogin = true
                if(!communicationManager.sendNetworkGetCars()){
                    showError(requireContext().getString(R.string.err_login_init_network))
                    logoutButtonAction()
                    btn_login.isEnabled = true
                }

            } else {
                showError(intent.getStringExtra(BroadcastKeys.KeyErrorMessage.name))
            }
        }
    }

    private val getCarsReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "getCarsReceiver.onReceive was triggered.")


            val error = intent.getStringExtra(BroadcastKeys.KeyErrorMessage.name)

            if (error == null || error.isBlank()) {
                val cars = intent.getSerializableExtra(BroadcastKeys.KeyCars.name) as ArrayList<*>
                showCarListDialog(cars)
                showLogout(input_username.text.toString())
                btn_login.isEnabled = true
            } else {
                // can not communicate with servers show error and logout
                showError(error)
                logoutButtonAction()
            }
        }
    }

    private val createCarReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "createCarReceiver.onReceive was triggered.")

            val error = intent.getStringExtra(BroadcastKeys.KeyErrorMessage.name)

            if (error == null || error.isBlank()) {
                communicationManager.networkLoginSuccess()
            } else {
                showError(error)
                logoutButtonAction()
            }
        }
    }

    private fun showLogin() {
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
            showError(requireContext().getText(R.string.err_login_invalid_inputs).toString())
            return
        }

        // show spinner
        if(!communicationManager.sendNetworkLogin(input_username.text.toString(), input_password.text.toString())){
            showError(requireContext().getString(R.string.err_login_init_network))
        }
    }

    private fun logoutButtonAction() {
        btn_logout.isEnabled = false
        btn_login.isEnabled = true

        Thread(Runnable {
            Storage.getInstance(requireContext()).clearAllTables()
            communicationContext.isLogin = false
            btn_logout.post { btn_logout.isEnabled = true }
            showLogin()
        }).start()
    }

    private fun validInputs(): Boolean {
        return !input_username.text.isBlank() && !input_password.text.isBlank()
    }

    private fun showCarListDialog(cars: List<*>) {
        val alertDialog: AlertDialog? = activity?.let {

            val names: List<CharSequence> = cars.map { car -> (car as LinkedTreeMap<*, *>)["name"] as CharSequence }

            AlertDialog.Builder(it)
                    .setTitle(R.string.select_car)
                    .setCancelable(false)
                    .setItems(names.toTypedArray()
                    ) { _, i ->
                        // Save selected car to DB in entity user
                        Thread(Runnable {
                            val storageUserService = Storage.getInstance(requireContext()).userService
                            val user = storageUserService.getUser()

                            if (user != null) {
                                val car = cars[i] as LinkedTreeMap<*, *>
                                user.carName = car["name"] as String
                                user.carId = (car["id"] as Double).toLong()
                                storageUserService.updateUser(user)
                                communicationManager.networkLoginSuccess()
                            }
                        }).start()
                    }
                    .setNeutralButton(R.string.create_new_car) { _, _ -> showCreateCarDialog() }
                    .create()
        }

        alertDialog?.show()
    }

    private fun showCreateCarDialog() {
        Log.d(tag, "New car will be created")

        val alertDialog: AlertDialog? = activity?.let {

            val inflater = requireActivity().layoutInflater

            AlertDialog.Builder(it)
                    .setTitle(R.string.create_new_car)
                    .setCancelable(false)
                    .setView(inflater.inflate(R.layout.dialog_create_car, null))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        val name = (dialog as AlertDialog).car_dialog_name.text
                        if (name.isBlank()) {
                            showCreateCarDialog()
                        } else {
                            if(!communicationManager.sendNetworkCreateCar(name.toString())){
                                showError(requireContext().getString(R.string.err_login_init_network))
                                logoutButtonAction()
                            }
                        }
                    }
                    .create()
        }

        alertDialog?.show()
    }

    private fun showError(text: String){
        login_error_text_view.text = text
        login_error_text_view.visibility = View.VISIBLE
        btn_login.isEnabled = true
    }
}