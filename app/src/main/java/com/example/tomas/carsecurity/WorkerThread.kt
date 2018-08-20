package com.example.tomas.carsecurity

import android.os.Handler
import android.os.HandlerThread

class WorkerThread(name: String) : HandlerThread(name) {

    private lateinit var handler: Handler

    fun postTask(task: Runnable){
        handler.post(task)
    }

    fun prepareHandler(){
        handler = Handler(looper)
    }
}