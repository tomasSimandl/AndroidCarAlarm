package com.example.tomas.carsecurity.fragments

import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.communication.network.NetworkProvider
import com.example.tomas.carsecurity.storage.Storage
import kotlinx.android.synthetic.main.status_fragment.*

/**
 * Class represents status view.
 */
class StatusFragment : Fragment() {

    /**
     * Set status view and all text views.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.status_fragment, container, false)
    }

    /**
     * Method sets texts to all status messages on page.
     */
    override fun onResume() {
        super.onResume()

        val handler = Handler()

        Thread {
            val storage = Storage.getInstance(requireContext())
            val messageCount = storage.messageService.countMessages(NetworkProvider.hashCode())
            var routeCount = storage.routeService.countRoutes()
            val locationCount = storage.locationService.countLocations()

            if (locationCount == 0L) routeCount = 0L

            handler.post {
                status_event_message_text_view.text =
                        getString(R.string.status_fragment_event_message, messageCount)

                status_route_message_text_view.text =
                        getString(R.string.status_fragment_route_message, routeCount)

                status_position_message_text_view.text =
                        getString(R.string.status_fragment_position_message, locationCount)
            }
        }.start()
    }
}