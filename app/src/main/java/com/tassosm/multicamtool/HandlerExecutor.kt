package com.tassosm.multicamtool

import android.os.Handler
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

class HandlerExecutor(h: Handler) : Executor {

    private val handler = h

    override fun execute(command: Runnable) {
        if (!handler.post(command)) {
            throw RejectedExecutionException("$handler is shutting down");
        }
    }
}