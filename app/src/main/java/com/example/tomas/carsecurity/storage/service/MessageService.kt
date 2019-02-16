package com.example.tomas.carsecurity.storage.service

import com.example.tomas.carsecurity.storage.AppDatabase
import com.example.tomas.carsecurity.storage.entity.Message

class MessageService(private val database: AppDatabase) {
    fun saveMessage(message: Message) {
        database.messageDao().insert(message)
    }

    fun getMessages(communicatorHash: Int): List<Message> {
        val messages = database.messageDao().getAllByCommunicatorHash(communicatorHash)
        database.messageDao().delete(messages)

        return messages
    }

    fun deleteMessages(communicatorHash: Int) {
        database.messageDao().deleteAllByCommunicatorHash(communicatorHash)
    }

    fun deleteMessage(message: Message) {
        database.messageDao().delete(message)
    }
}