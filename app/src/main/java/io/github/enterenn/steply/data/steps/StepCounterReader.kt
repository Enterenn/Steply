package io.github.enterenn.steply.data.steps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlin.coroutines.resume
import kotlin.math.roundToLong
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

sealed interface StepCounterRead {
    data class Value(val cumulativeSteps: Long) : StepCounterRead
    data object Unavailable : StepCounterRead
    data object TimedOut : StepCounterRead
}

class StepCounterReader(context: Context) {
    private val sensorManager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var liveListener: SensorEventListener? = null

    fun startListening(onValue: (Long) -> Unit): Boolean {
        stopListening()
        val manager = sensorManager ?: return false
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.isEmpty()) return
                onValue(event.values[0].roundToLong())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val registered = try {
            manager.registerListener(
                listener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL,
                mainHandler,
            )
        } catch (_: SecurityException) {
            false
        }
        if (!registered) return false
        liveListener = listener
        return true
    }

    fun stopListening() {
        val listener = liveListener ?: return
        liveListener = null
        sensorManager?.unregisterListener(listener)
    }

    suspend fun read(): StepCounterRead {
        val manager = sensorManager ?: return StepCounterRead.Unavailable
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            ?: return StepCounterRead.Unavailable
        val result = withTimeoutOrNull(READ_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        manager.unregisterListener(this)
                        if (continuation.isActive) {
                            continuation.resume(StepCounterRead.Value(event.values[0].roundToLong()))
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                val registered = try {
                    manager.registerListener(
                        listener,
                        sensor,
                        SensorManager.SENSOR_DELAY_NORMAL,
                        mainHandler,
                    )
                } catch (_: SecurityException) {
                    false
                }
                if (!registered) {
                    if (continuation.isActive) continuation.resume(StepCounterRead.Unavailable)
                    return@suspendCancellableCoroutine
                }
                continuation.invokeOnCancellation { manager.unregisterListener(listener) }
            }
        }
        return result ?: StepCounterRead.TimedOut
    }

    private companion object {
        const val READ_TIMEOUT_MS = 5_000L
    }
}
