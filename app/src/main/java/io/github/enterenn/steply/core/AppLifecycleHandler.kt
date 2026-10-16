package io.github.enterenn.steply.core

/**
 * permission retirée au premier plan : stop live, un evaluate, pas de lecture ;
 * permission accordée : enqueue, live, une lecture, pas de compaction ;
 * arrière-plan : stop live ;
 * Doze : le worker peut attendre, pas de foreground service ni de contournement ;
 * app tuée : seul le worker, le handler ne tourne pas.
 */
class AppLifecycleHandler(
    private val hasPermission: () -> Boolean,
    private val publishGranted: (Boolean) -> Unit,
    private val stopLive: () -> Unit,
    private val startLive: () -> Unit,
    private val enqueue: () -> Unit,
    private val recordReading: () -> Unit,
    private val evaluate: () -> Unit,
) {
    fun onForeground() {
        val granted = hasPermission()
        publishGranted(granted)
        if (!granted) {
            stopLive()
            evaluate()
            return
        }
        enqueue()
        startLive()
        recordReading()
    }

    fun onBackground() {
        stopLive()
    }
}
