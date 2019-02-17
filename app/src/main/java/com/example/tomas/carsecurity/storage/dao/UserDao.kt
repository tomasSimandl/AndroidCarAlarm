package com.example.tomas.carsecurity.storage.dao

import android.arch.persistence.room.*
import com.example.tomas.carsecurity.storage.entity.User

@Dao
interface UserDao {

    @Query("SELECT username FROM user LIMIT 1")
    fun getUsername(): String

    @Query("SELECT * FROM user LIMIT 1")
    fun get(): User?

    @Insert
    fun insert(user: User)

    @Delete
    fun delete(user: User)

    @Update
    fun update(user: User)
}