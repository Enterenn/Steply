package io.github.enterenn.steply.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class DayStepsTest {
    private val zone = ZoneOffset.UTC
    private val day = LocalDate.of(2026, 10, 15)

    @Test
    fun sumsSimpleDelta() {
        val readings = listOf(
            reading(100, "2026-10-15T10:00:00Z"),
            reading(140, "2026-10-15T11:00:00Z"),
        )
        assertEquals(40L, daySteps(readings, day, zone))
    }

    @Test
    fun countsResetFromZero() {
        val readings = listOf(
            reading(5_000, "2026-10-15T10:00:00Z"),
            reading(12, "2026-10-15T11:00:00Z"),
        )
        assertEquals(12L, daySteps(readings, day, zone))
    }

    @Test
    fun returnsZeroWhenDayIsEmpty() {
        assertEquals(0L, daySteps(emptyList(), day, zone))
    }

    @Test
    fun ignoresIntervalOutsideDay() {
        val readings = listOf(
            reading(100, "2026-10-14T10:00:00Z"),
            reading(180, "2026-10-14T12:00:00Z"),
        )
        assertEquals(0L, daySteps(readings, day, zone))
    }

    @Test
    fun splitsIntervalAcrossMidnight() {
        // 100 pas, 1 h avant minuit et 2 h après : 33 + 67.
        val readings = listOf(
            reading(1_000, "2026-10-14T23:00:00Z"),
            reading(1_100, "2026-10-15T02:00:00Z"),
        )
        assertEquals(33L, daySteps(readings, day.minusDays(1), zone))
        assertEquals(67L, daySteps(readings, day, zone))
    }

    @Test
    fun sumsSeveralReadings() {
        val readings = listOf(
            reading(1_000, "2026-10-15T08:00:00Z"),
            reading(1_040, "2026-10-15T12:00:00Z"),
            reading(1_100, "2026-10-15T18:00:00Z"),
        )
        assertEquals(100L, daySteps(readings, day, zone))
    }

    private fun reading(cumulative: Long, instant: String) =
        StepReading(cumulative, Instant.parse(instant))
}
