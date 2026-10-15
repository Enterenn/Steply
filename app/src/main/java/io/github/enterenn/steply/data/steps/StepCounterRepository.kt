package io.github.enterenn.steply.data.steps

import io.github.enterenn.steply.data.db.AppDatabase
import io.github.enterenn.steply.data.db.StepCounterReadingDao
import io.github.enterenn.steply.data.db.StepCounterReadingEntity
import io.github.enterenn.steply.data.steps.trendMonthBars as aggregateMonths
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.IdentityHashMap
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
    val lastOptimization: String? = null,
)

class StepCounterRepository(
    private val dao: StepCounterReadingDao,
    private val reader: StepCounterReader,
    private val database: AppDatabase,
    private val settings: SettingsStore,
) {
    private val gate = Mutex()
    private val cacheLock = Mutex()
    private val cachedReadings = mutableListOf<StepReading>()
    private var cacheLoaded = false
    private var lastOptimization: String? = null
    private var lastOptimizationLoaded = false
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

    suspend fun trendBars(
        dayCount: Int,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<TrendDayBar> {
        val readings = readingsSnapshot()
        return withContext(Dispatchers.Default) { trendDayBars(readings, today, dayCount, zone) }
    }

    suspend fun trendMonthBars(
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<TrendMonthBar> {
        val readings = readingsSnapshot()
        return withContext(Dispatchers.Default) { aggregateMonths(readings, today, zone) }
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

    suspend fun readingCount(): Long = dao.count()

    suspend fun exportCsv(): String = gate.withLock {
        writeStepCsv(storedReadings())
    }

    suspend fun importCsv(content: String): StepCsvParse = gate.withLock {
        val parsed = parseStepCsv(content)
        if (parsed is StepCsvParse.Accepted) {
            val now = Instant.now()
            val hardware = lastLiveCumulative ?: _state.value.snapshot?.cumulativeSteps
            val zone = ZoneId.systemDefault().rules.getOffset(now).toStoredOffset()
            storeReadings(anchorReadings(parsed.readings, hardware, now, zone))
            compactLocked()
        }
        parsed
    }

    suspend fun compactStoredReadings() = gate.withLock {
        compactLocked()
    }

    suspend fun purgeSteps() = gate.withLock {
        dao.purgeSteps()
        cacheLock.withLock {
            cachedReadings.clear()
            cacheLoaded = true
        }
        publishLatest()
    }

    private suspend fun storedReadings(): List<CsvReading> = dao.allById().map { reading ->
        CsvReading(
            cumulativeSteps = reading.cumulativeSteps,
            recordedAt = Instant.parse(reading.recordedAt),
            zoneOffset = reading.zoneOffset,
        )
    }

    private suspend fun storeReadings(readings: List<CsvReading>) {
        dao.replaceAll(
            readings.map { reading ->
                StepCounterReadingEntity(
                    cumulativeSteps = reading.cumulativeSteps,
                    recordedAt = reading.recordedAt.toString(),
                    zoneOffset = reading.zoneOffset,
                )
            },
        )
        cacheLock.withLock {
            cachedReadings.clear()
            cachedReadings += readings.map { StepReading(it.cumulativeSteps, it.recordedAt) }
            cacheLoaded = true
        }
        publishLatest()
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

    private suspend fun compactLocked() {
        val stored = dao.allById()
        if (stored.size < 2) return
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val projected = stored.map { StepReading(it.cumulativeSteps, Instant.parse(it.recordedAt)) }
        val compacted = compactReadings(projected, zone, now)
        if (compacted.size >= stored.size) return
        val positions = IdentityHashMap<StepReading, Int>(projected.size)
        for (index in projected.indices) positions[projected[index]] = index
        val kept = ArrayList<CsvReading>(compacted.size)
        for (reading in compacted) {
            val index = positions[reading] ?: return
            val entity = stored[index]
            kept.add(CsvReading(entity.cumulativeSteps, reading.recordedAt, entity.zoneOffset))
        }
        storeReadings(kept)
        withContext(Dispatchers.IO) {
            // replaceAll est déjà commis. VACUUM échoue dans une transaction ouverte.
            database.openHelper.writableDatabase.execSQL("VACUUM")
        }
        val recorded = now.toString()
        settings.setLastOptimization(now)
        lastOptimization = recorded
        lastOptimizationLoaded = true
        publishLatest()
    }

    private suspend fun publishLatest() {
        if (!lastOptimizationLoaded) {
            lastOptimization = settings.lastOptimization()
            lastOptimizationLoaded = true
        }
        val latest = dao.latestTwo()
        val current = latest.getOrNull(0)
        if (current == null) {
            _state.update { it.copy(snapshot = null, lastOptimization = lastOptimization) }
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
                lastOptimization = lastOptimization,
            )
        }
    }
}

private fun ZoneOffset.toStoredOffset(): String = if (totalSeconds == 0) "+00:00" else id
