package com.example.tomas.carsecurity.communication

import com.example.tomas.carsecurity.storage.entity.Location
import com.example.tomas.carsecurity.tools.ToolsEnum

/**
 * Represent API of all communication providers.
 */
interface ICommunicationProvider {

    // initialization status can be changed only via methods initialize and destroy called
    // from [CommunicationManager]
    /**
     * Method initialized this provider
     */
    fun initialize(): Boolean

    /**
     * Method deinitialized this provider.
     */
    fun destroy()

    /**
     * Return if provider is initialized.
     * @return if provider is initialized.
     */
    fun isInitialize(): Boolean

    /**
     * Send event message.
     *
     * @param messageType identification of which event should be send.
     * @param args arguments of message
     * @return true on success, false otherwise
     */
    fun sendEvent(messageType: MessageType, vararg args: String): Boolean

    /**
     * Send information message thant util was activate or deactivate
     *
     * @param toolsEnum identification which tools status was changed
     * @param enabled true - util was activated, false - util was deactivated
     * @return true on success, false otherwise
     */
    fun sendUtilSwitch(toolsEnum: ToolsEnum, enabled: Boolean): Boolean

    /**
     * Send message with input location.
     *
     * @param location actual device location which will be sent
     * @param isAlarm indication if device is in alarm mode
     * @param cache indicates if location should be cached or send immediately
     * @return true on success, false otherwise
     */
    fun sendLocation(location: Location, isAlarm: Boolean, cache: Boolean = false): Boolean

    /**
     * Send status composed of input parameters.
     *
     * @param battery percentage status of battery level
     * @param isCharging indication if device is connected to external source of power
     * @param tools list of activated tools
     * @return true on success, false otherwise
     */
    fun sendStatus(battery: Float, isCharging: Boolean, powerSaveMode: Boolean, tools: Map<ToolsEnum, Boolean>): Boolean

    /**
     * Send create route request.
     * @param localRouteId id of route stored in local Room db
     * @return true on success, false otherwise
     */
    fun sendRoute(localRouteId: Int): Boolean
}