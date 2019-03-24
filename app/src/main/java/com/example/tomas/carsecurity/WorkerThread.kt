package com.example.tomas.carsecurity

import android.os.Handler
import android.os.HandlerThread

/**
 * Representation of thread to which can be append tasks. When no tasks are available thread
 * withs for new tasks.
 *
 * @param name of thread
 */
class WorkerThread(name: String) : HandlerThread(name) {

    /** Handler used for post tasks to thread */
    private lateinit var handler: Handler

    /**
     * Append task to this threads queue of tasks. Thread will be run when all tasks in queue
     * before will be processed.
     *
     * @param task which will be run in this thread.
     */
    fun postTask(task: Runnable) {
        handler.post(task)
    }

    /**
     * Initialization of this class
     */
    fun prepareHandler() {
        handler = Handler(looper)
    }
}