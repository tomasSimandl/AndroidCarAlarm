package com.example.tomas.carsecurity.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

/**
 * Class represents login view and part of login logic.
 */
class LoginFragment : Fragment() {

    /**
     * Enum of broadcast keys which can be used to communicate with this class over [BroadcastReceiver].
     */
    enum class BroadcastKeys {
        BroadcastLoginResult, BroadcastGetCarsResult, BroadcastCreateCarsResult, KeySuccess, KeyErrorMessage, KeyCars
    }

    /** Instance of [CommunicationManager] used for sending login requests. */
    private lateinit var communicationManager: CommunicationManager
    /** Instance of [CommunicationContext] */
    private lateinit var communicationContext: CommunicationContext

    /**
     * Set login view and listeners to buttons in view.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.login_fragment, container, false)
        view.btn_login.setOnClickListener { loginButtonAction() }
        view.btn_logout.setOnClickListener { logoutButtonAction() }

        return view
    }

    /**
     * Initialize [communicationManager] and [communicationContext]. Register this class to [BroadcastReceiver] and
     * decide if should be visible login or logout view.
     */
    override fun onResume() {
        super.onResume()

        if (!::communicationContext.isInitialized) communicationContext = CommunicationContext(requireContext())
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
        broadcastManager.registerReceiver(loginReceiver, IntentFilter(BroadcastKeys.BroadcastLoginResult.name))
        broadcastManager.registerReceiver(getCarsReceiver, IntentFilter(BroadcastKeys.BroadcastGetCarsResult.name))
        broadcastManager.registerReceiver(createCarReceiver, IntentFilter(BroadcastKeys.BroadcastCreateCarsResult.name))
    }

    /**
     * Unregister this class from [BroadcastReceiver].
     */
    override fun onStop() {
        super.onStop()

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(loginReceiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(getCarsReceiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(createCarReceiver)
    }

    /**
     * Receiver which handle results of login request to communication manager. On success login method change view
     * and send request to list users cars. Otherwise show error.
     */
    private val loginReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "BroadcastReceiver.onReceive was triggered.")

            input_password.text.clear()

            if (intent.getBooleanExtra(BroadcastKeys.KeySuccess.name, false)) {
                communicationContext.isLogin = true
                if (!communicationManager.sendNetworkGetCars()) {
                    showError(requireContext().getString(R.string.err_login_init_network))
                    logoutButtonAction()
                    btn_login.isEnabled = true
                }

            } else {
                showError(intent.getStringExtra(BroadcastKeys.KeyErrorMessage.name))
            }
        }
    }

    /**
     * Receiver which handle results of get list of users cars. On success show list to user. Otherwise show error.
     */
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

    /**
     * Receiver which handle create new car request. On success inform [communicationManager]. Otherwise show error.
     */
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

    /**
     * Method show login view.
     */
    private fun showLogin() {
        logout_view.post { logout_view.visibility = View.GONE }
        login_view.post { login_view.visibility = View.VISIBLE }
    }

    /**
     * Method show logout view and display message with users name in view.
     *
     * @param username is name of actual login user.
     */
    private fun showLogout(username: String) {
        logout_view.post { logout_view.visibility = View.VISIBLE }
        login_view.post { login_view.visibility = View.GONE }
        login_user_text_view.post {
            login_user_text_view.text = requireContext().getString(R.string.login_user_text, username)
        }
    }

    /**
     * Method represents login button action. Form inputs are validate. On success validation login request is send
     * to [communicationManager].
     */
    private fun loginButtonAction() {

        btn_login.isEnabled = false
        login_error_text_view.visibility = View.GONE

        if (!validInputs()) {
            showError(requireContext().getText(R.string.err_login_invalid_inputs).toString())
            return
        }

        // show spinner
        if (!communicationManager.sendNetworkLogin(input_username.text.toString(), input_password.text.toString())) {
            showError(requireContext().getString(R.string.err_login_init_network))
        }
    }

    /**
     * Method represents logout button action. Database with all users data is cleared.
     */
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

    /**
     * Method returns if form inputs on login view are valid for sending of login request.
     * @return true if inputs are valid, false otherwise.
     */
    private fun validInputs(): Boolean {
        return !input_username.text.isBlank() && !input_password.text.isBlank()
    }

    /**
     * Method display dialog of actual users cars and with option to create a new car.
     *
     * @param cars is list of cars which will be displayed. Car should be LinkedTreeMap with attribute name.
     */
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

    /**
     * Method show dialog to create new car. Non empty name is than sand to server over [communicationManager].
     */
    @SuppressLint("InflateParams")
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
                            if (!communicationManager.sendNetworkCreateCar(name.toString())) {
                                showError(requireContext().getString(R.string.err_login_init_network))
                                logoutButtonAction()
                            }
                        }
                    }
                    .create()
        }

        alertDialog?.show()
    }

    /**
     * Method display [text] as error message and allow click to login button.
     * @param text is error message which will be visible to user.
     */
    private fun showError(text: String) {
        login_error_text_view.text = text
        login_error_text_view.visibility = View.VISIBLE
        btn_login.isEnabled = true
    }
}