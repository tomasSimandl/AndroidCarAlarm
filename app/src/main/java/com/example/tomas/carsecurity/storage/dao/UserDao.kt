package com.example.tomas.carsecurity.storage.dao

import android.arch.persistence.room.*
import com.example.tomas.carsecurity.storage.entity.User

/**
 * Data access object to table user in Room database. It is expected than in database will be only one user at a time.
 */
@Dao
interface UserDao {

    /**
     * Method return username of first user in database.
     * @return username of first user in database.
     */
    @Query("SELECT username FROM user LIMIT 1")
    fun getUsername(): String

    /**
     * Method returns first user from database.
     * @return first user from database.
     */
    @Query("SELECT * FROM user LIMIT 1")
    fun get(): User?

    /**
     * Method insert given user to database.
     * @param user which will be saved in database.
     */
    @Insert
    fun insert(user: User)

    /**
     * Method delete input user from database.
     * @param user which will be deleted from database.
     */
    @Delete
    fun delete(user: User)

    /**
     * Method updates existing user in database.
     * @param user which will be updated in database.
     */
    @Update
    fun update(user: User)
}