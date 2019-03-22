package com.example.tomas.carsecurity.storage.service

import com.example.tomas.carsecurity.storage.AppDatabase
import com.example.tomas.carsecurity.storage.entity.User

/**
 * Service which only call [database] dao objects associated with user table.
 * @param database is open Room database.
 */
class UserService(private val database: AppDatabase) {

    /**
     * Method insert given user to database.
     * @param user which will be saved in database.
     */
    fun saveUser(user: User) = database.userDao().insert(user)

    /**
     * Method updates existing user in database.
     * @param user which will be updated in database.
     */
    fun updateUser(user: User) = database.userDao().update(user)

    /**
     * Method returns first user from database.
     * @return first user from database.
     */
    fun getUser(): User? = database.userDao().get()

    /**
     * Method return username of first user in database.
     * @return username of first user in database.
     */
    fun getUsername(): String = database.userDao().getUsername()

    /**
     * Method delete input user from database.
     * @param user which will be deleted from database.
     */
    fun deleteUser(user: User) = database.userDao().delete(user)
}