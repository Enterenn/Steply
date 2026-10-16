package io.github.enterenn.steply.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLifecycleHandlerTest {
    private val calls = mutableListOf<String>()

    private fun handler(granted: Boolean) = AppLifecycleHandler(
        hasPermission = { granted },
        publishGranted = { value -> calls += "publish:$value" },
        stopLive = { calls += "stop" },
        startLive = { calls += "start" },
        enqueue = { calls += "enqueue" },
        recordReading = { calls += "read" },
        evaluate = { calls += "evaluate" },
    )

    @Test
    fun foregroundDeniedStopsLiveAndEvaluatesOnce() {
        handler(granted = false).onForeground()
        assertEquals(listOf("publish:false", "stop", "evaluate"), calls)
    }

    @Test
    fun foregroundGrantedEnqueuesStartsLiveAndReads() {
        handler(granted = true).onForeground()
        assertEquals(listOf("publish:true", "enqueue", "start", "read"), calls)
    }

    @Test
    fun backgroundStopsLiveOnly() {
        handler(granted = true).onBackground()
        assertEquals(listOf("stop"), calls)
    }
}
