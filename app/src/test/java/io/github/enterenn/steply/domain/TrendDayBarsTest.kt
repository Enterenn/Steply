package io.github.enterenn.steply.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class TrendDayBarsTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 15)

    @Test
    fun sevenDaysIncludeAnEmptyDay() {
        val readings = listOf(
            reading(0, "2026-10-09T08:00:00Z"),
            reading(40, "2026-10-09T10:00:00Z"),
            reading(40, "2026-10-11T08:00:00Z"),
            reading(70, "2026-10-11T09:00:00Z"),
            reading(70, "2026-10-15T08:00:00Z"),
            reading(90, "2026-10-15T09:00:00Z"),
        )
        val bars = trendDayBars(readings, today, 7, zone)

        assertEquals(7, bars.size)
        assertEquals(today.minusDays(6), bars.first().day)
        assertEquals(40L, bars[0].steps)
        assertEquals(0L, bars[1].steps)
        assertEquals(30L, bars[2].steps)
        assertEquals(20L, bars.last().steps)
    }

    @Test
    fun thirtyDays() {
        val readings = listOf(
            reading(0, "2026-09-16T00:00:00Z"),
            reading(30, "2026-09-16T06:00:00Z"),
        )
        val bars = trendDayBars(readings, today, 30, zone)

        assertEquals(30, bars.size)
        assertEquals(LocalDate.of(2026, 9, 16), bars.first().day)
        assertEquals(30L, bars.first().steps)
        assertEquals(0L, bars[1].steps)
        assertEquals(0L, bars.last().steps)
    }

    @Test
    fun todayIsTheLastBar() {
        val readings = listOf(
            reading(0, "2026-10-15T08:00:00Z"),
            reading(15, "2026-10-15T09:00:00Z"),
        )
        val bars = trendDayBars(readings, today, 7, zone)

        assertEquals(today.minusDays(1), bars[bars.lastIndex - 1].day)
        assertEquals(today, bars.last().day)
        assertEquals(0L, bars[bars.lastIndex - 1].steps)
        assertEquals(15L, bars.last().steps)
    }

    @Test
    fun oneGoalSetsTheAxis() {
        val goals = listOf(8_000)
        assertEquals(8_400, trendChartMax(4_000L, goals))
        assertEquals(listOf(0, 8_000), trendAxisTickValues(8_400, goals))
    }

    @Test
    fun twoGoalsAddATickForEach() {
        val goals = listOf(5_000, 8_000)
        assertEquals(13_000, trendChartMax(12_000L, goals))
        assertEquals(listOf(0, 5_000, 8_000, 13_000), trendAxisTickValues(13_000, goals))
    }

    @Test
    fun goalAboveTheAxisTopIsNotATick() {
        assertEquals(listOf(0), trendAxisTickValues(8_000, listOf(10_000)))
    }

    private fun reading(cumulative: Long, instant: String) =
        StepReading(cumulative, Instant.parse(instant))
}
