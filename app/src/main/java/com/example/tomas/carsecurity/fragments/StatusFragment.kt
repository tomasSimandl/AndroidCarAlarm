package com.example.tomas.carsecurity.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SimpleAdapter
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.network.NetworkProvider
import com.example.tomas.carsecurity.context.CommunicationContext
import com.example.tomas.carsecurity.storage.Storage
import kotlinx.android.synthetic.main.status_fragment.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Class represents status view.
 */
class StatusFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    /** Instance of [CommunicationContext] */
    private lateinit var communicationContext: CommunicationContext

    /**
     * Set status view and all text views.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.status_fragment, container, false)
    }

    /**
     * Method only initialize [communicationContext]
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        communicationContext = CommunicationContext(requireContext())
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {

        when (key) {
            requireContext().getString(R.string.key_communication_network_sync_status) -> {
                Log.d(tag, "Sync status was changed")
                updateStatus()
            }
        }
    }


    /**
     * Method sets texts to all status messages on page and register this class to on preferenceChanged listener.
     */
    override fun onResume() {
        super.onResume()

        status_layout_refresh.setOnRefreshListener { updateStatus() }

        communicationContext.registerOnPreferenceChanged(this)
        updateStatus()
    }


    /**
     * Method only unregister this class from OnPreferenceChanged.
     */
    override fun onStop() {
        super.onStop()
        communicationContext.unregisterOnPreferenceChanged(this)
    }

    private fun updateStatus() {

        status_layout.post {
            status_sync_status.text = getString(
                    R.string.status_fragment_sync_status,
                    communicationContext.synchronizationStatus.name
            )
        }

        Thread {
            val storage = Storage.getInstance(requireContext())

            val listItems = ArrayList<HashMap<String, String>>()

            storage.routeService.getRoutes().forEach {
                val positionSize = storage.locationService.getLocationsByLocalRouteId(it.uid).size

                if (positionSize != 0) {
                    val format = SimpleDateFormat.getDateTimeInstance()
                    val time = format.format(Date(it.time))

                    val map = HashMap<String, String>(2)
                    map["first"] = time
                    map["second"] = "positions: $positionSize"
                    listItems.add(map)
                }
            }

            if (listItems.isEmpty()){
                val map = HashMap<String, String>(2)
                map["first"] = "No local route"
                map["second"] = ""
                listItems.add(map)
            }

            val adapter = SimpleAdapter(
                    requireContext(),
                    listItems,
                    android.R.layout.simple_list_item_2,
                    arrayOf("first", "second"),
                    arrayOf(android.R.id.text1, android.R.id.text2).toIntArray()
            )

            status_layout_refresh.post {
                routes_list.adapter = adapter
                status_layout_refresh.isRefreshing = false
            }

        }.start()
    }
}