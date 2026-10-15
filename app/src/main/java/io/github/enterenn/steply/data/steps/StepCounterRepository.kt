package io.github.enterenn.steply.data.steps

import io.github.enterenn.steply.data.db.StepCounterReadingDao
import io.github.enterenn.steply.data.db.StepCounterReadingEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class StepCounterSnapshot(
    val cumulativeSteps: Long,
    val recordedAt: String,
    val zoneOffset: String,
    val deltaSteps: Long?,
    val counterReset: Boolean,
)

enum class StepReadStatus {
    Idle,
    SensorMissing,
    TimedOut,
}

data class StepReadingsState(
    val snapshot: StepCounterSnapshot? = null,
    val status: StepReadStatus = StepReadStatus.Idle,
)

class StepCounterRepository(
    private val dao: StepCounterReadingDao,
    private val reader: StepCounterReader,
) {
    private val gate = Mutex()
    private val cacheLock = Mutex()
    private val cachedReadings = mutableListOf<StepReading>()
    private var cacheLoaded = false
    private val _state = MutableStateFlow(StepReadingsState())
    val state: StateFlow<StepReadingsState> = _state.asStateFlow()
    private var lastLiveCumulative: Long? = null

    fun startLiveUpdates(scope: CoroutineScope) {
        lastLiveCumulative = null
        reader.startListening { cumulative ->
            if (cumulative == lastLiveCumulative) return@startListening
            lastLiveCumulative = cumulative
            scope.launch { recordCumulative(cumulative) }
        }
    }

    fun stopLiveUpdates() {
        lastLiveCumulative = null
        reader.stopListening()
    }

    suspend fun refreshFromDatabase() = gate.withLock {
        publishLatest()
    }

    suspend fun stepsOn(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
        val readings = readingsSnapshot()
        return withContext(Dispatchers.Default) { daySteps(readings, day, zone) }
    }

    suspend fun recordCumulative(cumulativeSteps: Long) = gate.withLock {
        writeCumulative(cumulativeSteps)
    }

    suspend fun recordReading() = gate.withLock {
        when (val read = reader.read()) {
            is StepCounterRead.Value -> writeCumulative(read.cumulativeSteps)
            StepCounterRead.Unavailable -> _state.update { it.copy(status = StepReadStatus.SensorMissing) }
            StepCounterRead.TimedOut -> _state.update { it.copy(status = StepReadStatus.TimedOut) }
        }
    }

    private suspend fun writeCumulative(cumulativeSteps: Long) {
        val latest = dao.latestTwo().firstOrNull()
        if (latest?.cumulativeSteps == cumulativeSteps) return
        val now = Instant.now()
        val recordedAt = now.toString()
        dao.insert(
            StepCounterReadingEntity(
                cumulativeSteps = cumulativeSteps,
                recordedAt = recordedAt,
                zoneOffset = ZoneId.systemDefault().rules.getOffset(now).toStoredOffset(),
            ),
        )
        rememberReading(StepReading(cumulativeSteps, now))
        publishLatest()
    }

    private suspend fun readingsSnapshot(): List<StepReading> = cacheLock.withLock {
        if (!cacheLoaded) {
            cachedReadings.clear()
            cachedReadings += dao.allById().map { reading ->
                StepReading(reading.cumulativeSteps, Instant.parse(reading.recordedAt))
            }
            cacheLoaded = true
        }
        cachedReadings.toList()
    }

    private suspend fun rememberReading(reading: StepReading) = cacheLock.withLock {
        if (!cacheLoaded) return@withLock
        val last = cachedReadings.lastOrNull()
        if (last?.cumulativeSteps == reading.cumulativeSteps && last.recordedAt == reading.recordedAt) return@withLock
        cachedReadings.add(reading)
    }

    private suspend fun publishLatest() {
        val latest = dao.latestTwo()
        val current = latest.getOrNull(0)
        if (current == null) {
            _state.update { it.copy(snapshot = null) }
            return
        }
        val previous = latest.getOrNull(1)
        val delta = stepDelta(previous?.cumulativeSteps, current.cumulativeSteps)
        _state.update {
            it.copy(
                snapshot = StepCounterSnapshot(
                    cumulativeSteps = current.cumulativeSteps,
                    recordedAt = current.recordedAt,
                    zoneOffset = current.zoneOffset,
                    deltaSteps = delta?.steps,
                    counterReset = delta?.counterReset == true,
                ),
                status = StepReadStatus.Idle,
            )
        }
    }
}

private fun ZoneOffset.toStoredOffset(): String = if (totalSeconds == 0) "+00:00" else id
