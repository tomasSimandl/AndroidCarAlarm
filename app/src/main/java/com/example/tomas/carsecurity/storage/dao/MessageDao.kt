package com.example.tomas.carsecurity.storage.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.example.tomas.carsecurity.storage.entity.Message

@Dao
interface MessageDao {

    @Query("SELECT * FROM message WHERE communicator = :communicatorHash")
    fun getAllByCommunicatorHash(communicatorHash: Int): List<Message>

    @Insert
    fun insert(message: Message)

    @Delete
    fun delete(vararg message: Message)

    @Delete
    fun delete(messages: List<Message>)

    @Query("DELETE FROM message WHERE communicator = :communicatorHash")
    fun deleteAllByCommunicatorHash(communicatorHash: Int)
}