package com.example.tomas.carsecurity.fragments

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDelegate
import android.support.v7.content.res.AppCompatResources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.context.UtilsContext
import com.example.tomas.carsecurity.utils.UtilsEnum
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.content_main.view.*

class LoginFragment : Fragment() {

    private lateinit var utilsContext: UtilsContext


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        utilsContext = UtilsContext(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.login_fragment, container, false)


        return view
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onStop() {
        super.onStop()

    }
}