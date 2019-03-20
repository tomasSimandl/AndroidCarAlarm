package com.example.tomas.carsecurity.communication

/**
 * Enum of all possible types of messages which chan be send over communicators.
 */
enum class MessageType {
    UtilSwitch, Alarm, AlarmLocation, Location, BatteryWarn, Status, PowerConnected, PowerDisconnected
}