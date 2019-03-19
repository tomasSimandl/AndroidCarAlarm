package com.example.tomas.carsecurity.communication.network

/**
 * Object carry urls of all endpoints which are required for network communication with servers.
 */
object Mapping {
    // Data server endpoints =======================================================================
    const val CAR_URL = "car"
    const val ROUTE_URL = "route"
    const val EVENT_URL = "event"
    const val STATUS_URL = "status"
    const val LOCATION_URL = "position"
    const val FIREBASE_TOKEN_URL = "token"

    // Authorization server endpoints ==============================================================
    const val LOGIN_URL = "oauth/token"
}