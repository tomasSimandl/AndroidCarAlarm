package com.example.tomas.carsecurity.storage.service

import com.example.tomas.carsecurity.storage.AppDatabase
import com.example.tomas.carsecurity.storage.entity.User

class UserService(private val database: AppDatabase) {


    fun saveUser(user: User) = database.userDao().insert(user)
    fun updateUser(user: User) = database.userDao().update(user)
    fun getUser(): User? = database.userDao().get()
    fun getUsername(): String = database.userDao().getUsername()
    fun deleteUser(user: User) = database.userDao().delete(user)
}