package com.example.tomas.carsecurity.storage.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.example.tomas.carsecurity.storage.entity.Message

/**
 * Data access object to table message in Room database.
 */
@Dao
interface MessageDao {

    /**
     * Return all messages associated with input communicator.
     * @param communicatorHash hash of communication provider
     * @return list off all messages associated with input communicator.
     */
    @Query("SELECT * FROM message WHERE communicator = :communicatorHash")
    fun getAllByCommunicatorHash(communicatorHash: Int): List<Message>

    /**
     * Method return number of all messages in database.
     * @param communicatorHash hash of communication provider
     * @return number of messages in database
     */
    @Query("SELECT COUNT(uid) FROM message WHERE communicator = :communicatorHash")
    fun count(communicatorHash: Int): Long

    /**
     * Method save input message to database.
     * @param message which will be saved to database.
     */
    @Insert
    fun insert(message: Message)

    /**
     * Method remove all input messages from database
     * @param message vararg of all messages which will be deleted.
     */
    @Delete
    fun delete(vararg message: Message)

    /**
     * Method delete all messages from database which are specified by input list.
     * @param messages list of messages which will be deleted from database.
     */
    @Delete
    fun delete(messages: List<Message>)

    /**
     * Method delete all messages of given communicator.
     * @param communicatorHash hash of communicator provider of which messages will be deleted.
     */
    @Query("DELETE FROM message WHERE communicator = :communicatorHash")
    fun deleteAllByCommunicatorHash(communicatorHash: Int)
}