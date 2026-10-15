package io.github.enterenn.steply.data.steps

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingCompactionTest {
    private val now = Instant.parse("2026-10-15T12:00:00Z")

    @Test
    fun emptyHistoryStaysEmpty() {
        assertEquals(emptyList<StepReading>(), compactReadings(emptyList(), ZoneOffset.UTC, now))
    }

    @Test
    fun keepsEveryReadingInsideFortyEightHours() {
        val readings = listOf(
            reading(0, "2026-10-15T08:00:00Z"),
            reading(10, "2026-10-15T08:20:00Z"),
            reading(25, "2026-10-15T08:40:00Z"),
            reading(40, "2026-10-15T11:50:00Z"),
        )
        assertEquals(readings, compactReadings(readings, ZoneOffset.UTC, now))
    }

    @Test
    fun thinsOldMonotonicDayWithoutChangingSteps() {
        val zone = ZoneOffset.UTC
        val day = LocalDate.of(2026, 9, 1)
        val readings = (0..23).map { hour ->
            StepReading(hour * 100L, day.atTime(hour, 15).toInstant(ZoneOffset.UTC))
        }
        val compacted = compactReadings(readings, zone, now)
        assertTrue(compacted.size < readings.size)
        assertEquals(readings.first(), compacted.first())
        assertEquals(readings.last(), compacted.last())
        assertEquals(daySteps(readings, day, zone), daySteps(compacted, day, zone))
    }

    @Test
    fun keepsMidnightSplit() {
        val zone = ZoneOffset.UTC
        val previous = LocalDate.of(2026, 9, 1)
        val day = previous.plusDays(1)
        val readings = listOf(
            reading(1_000, "2026-09-01T12:00:00Z"),
            reading(1_000, "2026-09-01T18:00:00Z"),
            reading(1_000, "2026-09-01T23:00:00Z"),
            reading(1_100, "2026-09-02T02:00:00Z"),
            reading(1_100, "2026-09-02T09:00:00Z"),
            reading(1_100, "2026-09-02T16:00:00Z"),
        )
        val compacted = compactReadings(readings, zone, now)
        assertTrue(compacted.size < readings.size)
        assertEquals(33L, daySteps(readings, previous, zone))
        assertEquals(67L, daySteps(readings, day, zone))
        assertEquals(33L, daySteps(compacted, previous, zone))
        assertEquals(67L, daySteps(compacted, day, zone))

        val recent = listOf(
            reading(1_000, "2026-10-04T23:15:00Z"),
            reading(1_000, "2026-10-04T23:30:00Z"),
            reading(1_000, "2026-10-04T23:45:00Z"),
            reading(1_000, "2026-10-05T00:10:00Z"),
            reading(1_100, "2026-10-05T00:50:00Z"),
        )
        val thinned = compactReadings(recent, zone, now)
        assertFalse(thinned.any { it.recordedAt == Instant.parse("2026-10-04T23:30:00Z") })
        assertTrue(thinned.any { it.recordedAt == Instant.parse("2026-10-05T00:10:00Z") })
        assertEquals(0L, daySteps(thinned, LocalDate.of(2026, 10, 4), zone))
        assertEquals(100L, daySteps(thinned, LocalDate.of(2026, 10, 5), zone))
    }

    @Test
    fun keepsOldCounterReset() {
        val zone = ZoneOffset.UTC
        val day = LocalDate.of(2026, 8, 1)
        val readings = listOf(
            reading(5_000, "2026-08-01T08:00:00Z"),
            reading(12, "2026-08-01T09:00:00Z"),
            reading(20, "2026-08-01T10:00:00Z"),
            reading(40, "2026-08-01T18:00:00Z"),
        )
        val compacted = compactReadings(readings, zone, now)
        assertTrue(compacted.size < readings.size)
        assertTrue(compacted.any { it.cumulativeSteps == 5_000L })
        assertTrue(compacted.any { it.cumulativeSteps == 12L })
        assertFalse(compacted.any { it.cumulativeSteps == 20L })
        assertEquals(daySteps(readings, day, zone), daySteps(compacted, day, zone))
    }

    @Test
    fun keepsOldestAndNewest() {
        val readings = listOf(
            reading(100, "2026-10-05T14:00:00Z"),
            reading(140, "2026-10-05T14:20:00Z"),
            reading(180, "2026-10-05T14:40:00Z"),
            reading(300, "2026-10-05T18:00:00Z"),
            reading(300, "2026-10-15T11:00:00Z"),
        )
        val compacted = compactReadings(readings, ZoneOffset.UTC, now)
        assertEquals(readings.first(), compacted.first())
        assertEquals(readings.last(), compacted.last())
        assertTrue(compacted.size < readings.size)
    }

    @Test
    fun localHourDoesNotMoveDayTotal() {
        val zone = ZoneOffset.UTC
        val day = LocalDate.of(2026, 10, 5)
        val readings = listOf(
            reading(1_000, "2026-10-05T10:00:00Z"),
            reading(1_040, "2026-10-05T10:15:00Z"),
            reading(1_090, "2026-10-05T10:45:00Z"),
            reading(1_200, "2026-10-05T11:30:00Z"),
        )
        val compacted = compactReadings(readings, zone, now)
        assertFalse(compacted.any { it.recordedAt == Instant.parse("2026-10-05T10:15:00Z") })
        assertEquals(daySteps(readings, day, zone), daySteps(compacted, day, zone))
    }

    @Test
    fun keepsSumOfDaySteps() {
        val zone = ZoneOffset.UTC
        val start = LocalDate.of(2026, 9, 1)
        val readings = ArrayList<StepReading>()
        var cumulative = 0L
        for (offset in 0..2) {
            val day = start.plusDays(offset.toLong())
            for (hour in listOf(8, 12, 18, 22)) {
                cumulative += 50L
                readings += StepReading(cumulative, day.atTime(hour, 0).toInstant(ZoneOffset.UTC))
            }
        }
        val compacted = compactReadings(readings, zone, now)
        assertTrue(compacted.size < readings.size)
        assertEquals(sumOfDaySteps(readings, zone), sumOfDaySteps(compacted, zone))
        var day = start
        val lastDay = start.plusDays(2)
        while (!day.isAfter(lastDay)) {
            assertEquals(daySteps(readings, day, zone), daySteps(compacted, day, zone))
            day = day.plusDays(1)
        }
    }

    @Test
    fun usesLocalMidnightOutsideUtc() {
        val zone = ZoneId.of("Europe/Paris")
        val zonedNow = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, zone)
        val firstDay = LocalDate.of(2026, 9, 1)
        val secondDay = firstDay.plusDays(1)
        val readings = listOf(
            at(zone, firstDay, 12, 0, 0),
            at(zone, firstDay, 20, 0, 0),
            at(zone, firstDay, 23, 30, 0),
            at(zone, secondDay, 0, 30, 100),
            at(zone, secondDay, 8, 0, 100),
            at(zone, secondDay, 15, 0, 100),
        )
        val compacted = compactReadings(readings, zone, zonedNow.toInstant())
        assertTrue(compacted.size < readings.size)
        assertEquals(50L, daySteps(readings, firstDay, zone))
        assertEquals(50L, daySteps(readings, secondDay, zone))
        assertEquals(50L, daySteps(compacted, firstDay, zone))
        assertEquals(50L, daySteps(compacted, secondDay, zone))
        assertEquals(sumOfDaySteps(readings, zone), sumOfDaySteps(compacted, zone))
    }

    private fun reading(cumulative: Long, instant: String) =
        StepReading(cumulative, Instant.parse(instant))

    private fun at(zone: ZoneId, day: LocalDate, hour: Int, minute: Int, steps: Long) =
        StepReading(steps, day.atTime(hour, minute).atZone(zone).toInstant())

    private fun sumOfDaySteps(readings: List<StepReading>, zone: ZoneId): Long {
        if (readings.isEmpty()) return 0L
        var firstDay = readings.first().recordedAt.atZone(zone).toLocalDate()
        var lastDay = firstDay
        for (reading in readings) {
            val day = reading.recordedAt.atZone(zone).toLocalDate()
            if (day.isBefore(firstDay)) firstDay = day
            if (day.isAfter(lastDay)) lastDay = day
        }
        var day = firstDay
        var total = 0L
        while (!day.isAfter(lastDay)) {
            total += daySteps(readings, day, zone)
            day = day.plusDays(1)
        }
        return total
    }
}
