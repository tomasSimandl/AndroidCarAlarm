package com.example.tomas.carsecurity.storage.service

import com.example.tomas.carsecurity.storage.AppDatabase
import com.example.tomas.carsecurity.storage.entity.Message

/**
 * Service which only call [database] dao objects associated with message table.
 * @param database is open Room database.
 */
class MessageService(private val database: AppDatabase) {

    /**
     * Method save input message to database.
     * @param message which will be saved to database.
     */
    fun saveMessage(message: Message) {
        database.messageDao().insert(message)
    }

    /**
     * Return all messages associated with input communicator.
     * @param communicatorHash hash of communication provider
     * @return list off all messages associated with input communicator.
     */
    fun getMessages(communicatorHash: Int): List<Message> {
        return database.messageDao().getAllByCommunicatorHash(communicatorHash)
    }

    /**
     * Method delete all messages of given communicator.
     * @param communicatorHash hash of communicator provider of which messages will be deleted.
     */
    fun deleteMessages(communicatorHash: Int) {
        database.messageDao().deleteAllByCommunicatorHash(communicatorHash)
    }

    /**
     * Method remove input message from database
     * @param message which will be deleted.
     */
    fun deleteMessage(message: Message) {
        database.messageDao().delete(message)
    }
}