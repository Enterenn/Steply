package io.github.enterenn.steply.data.steps

import io.github.enterenn.steply.data.db.StepCounterReadingDao
import io.github.enterenn.steply.data.db.StepCounterReadingEntity
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val _state = MutableStateFlow(StepReadingsState())
    val state: StateFlow<StepReadingsState> = _state.asStateFlow()

    suspend fun refreshFromDatabase() = gate.withLock {
        publishLatest()
    }

    suspend fun recordReading() = gate.withLock {
        when (val read = reader.read()) {
            is StepCounterRead.Value -> {
                val now = Instant.now()
                dao.insert(
                    StepCounterReadingEntity(
                        cumulativeSteps = read.cumulativeSteps,
                        recordedAt = now.toString(),
                        zoneOffset = ZoneId.systemDefault().rules.getOffset(now).toStoredOffset(),
                    ),
                )
                publishLatest()
            }
            StepCounterRead.Unavailable -> _state.update { it.copy(status = StepReadStatus.SensorMissing) }
            StepCounterRead.TimedOut -> _state.update { it.copy(status = StepReadStatus.TimedOut) }
        }
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
