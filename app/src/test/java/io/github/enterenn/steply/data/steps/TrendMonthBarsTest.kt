package io.github.enterenn.steply.data.steps

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendMonthBarsTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 15)

    @Test
    fun twelveBarsEndOnTheCurrentMonth() {
        val bars = trendMonthBars(emptyList(), today, zone)

        assertEquals(TrendMonthCount, bars.size)
        assertEquals(LocalDate.of(2025, 11, 30), bars.first().lastDay)
        assertEquals(today, bars.last().lastDay)
        val months = bars.map { YearMonth.from(it.lastDay) }
        assertEquals(YearMonth.of(2025, 11), months.first())
        assertEquals(YearMonth.of(2026, 10), months.last())
        for (index in 1 until months.size) {
            assertEquals(months[index - 1].plusMonths(1), months[index])
        }
    }

    @Test
    fun aDayCountsOnlyInItsMonth() {
        val readings = listOf(
            reading(0, "2026-03-15T08:00:00Z"),
            reading(40, "2026-03-15T18:00:00Z"),
        )
        val bars = trendMonthBars(readings, today, zone)
        val march = YearMonth.of(2026, 3)

        assertEquals(40L, bars.single { YearMonth.from(it.lastDay) == march }.steps)
        assertEquals(0L, bars.filter { YearMonth.from(it.lastDay) != march }.sumOf { it.steps })
    }

    @Test
    fun monthBoundaryKeepsTheDayStepsSplit() {
        val readings = listOf(
            reading(1_000, "2026-09-30T23:00:00Z"),
            reading(1_100, "2026-10-01T02:00:00Z"),
        )
        val before = LocalDate.of(2026, 9, 30)
        val after = LocalDate.of(2026, 10, 1)
        val beforeSteps = daySteps(readings, before, zone)
        val afterSteps = daySteps(readings, after, zone)
        val bars = trendMonthBars(readings, today, zone)

        assertEquals(100L, beforeSteps + afterSteps)
        assertTrue(beforeSteps > 0L)
        assertTrue(afterSteps > 0L)
        assertEquals(beforeSteps, stepsIn(bars, YearMonth.of(2026, 9)))
        assertEquals(afterSteps, stepsIn(bars, YearMonth.of(2026, 10)))
    }

    @Test
    fun emptyHistoryIsTwelveZeros() {
        val bars = trendMonthBars(emptyList(), today, zone)

        assertEquals(List(TrendMonthCount) { 0L }, bars.map { it.steps })
    }

    @Test
    fun barSumEqualsSumOfDays() {
        val readings = listOf(
            reading(0, "2025-11-02T10:00:00Z"),
            reading(20, "2025-11-02T12:00:00Z"),
            reading(1_000, "2026-01-31T23:00:00Z"),
            reading(1_100, "2026-02-01T02:00:00Z"),
            reading(1_100, "2026-10-15T08:00:00Z"),
            reading(1_125, "2026-10-15T09:00:00Z"),
        )
        val bars = trendMonthBars(readings, today, zone)
        val days = windowDays()
        val dayTotal = days.sumOf { daySteps(readings, it, zone) }

        assertEquals(dayTotal, bars.sumOf { it.steps })
        for (bar in bars) {
            val month = YearMonth.from(bar.lastDay)
            val inMonth = days.filter { YearMonth.from(it) == month }.sumOf { daySteps(readings, it, zone) }
            assertEquals(inMonth, bar.steps)
        }
        val window = trendDayBars(readings, today, trendYearDayCount(today), zone)
        assertEquals(days.first(), window.first().day)
        assertEquals(today, window.last().day)
        assertEquals(dayTotal, window.sumOf { it.steps })
    }

    @Test
    fun currentMonthStopsToday() {
        val readings = listOf(
            reading(0, "2026-10-15T22:00:00Z"),
            reading(100, "2026-10-16T02:00:00Z"),
            reading(100, "2026-10-20T08:00:00Z"),
            reading(130, "2026-10-20T10:00:00Z"),
        )
        val tomorrow = today.plusDays(1)
        val later = LocalDate.of(2026, 10, 20)
        val todaySteps = daySteps(readings, today, zone)
        val bars = trendMonthBars(readings, today, zone)
        val october = windowDays().filter { YearMonth.from(it) == YearMonth.of(2026, 10) }

        assertTrue(daySteps(readings, tomorrow, zone) > 0L)
        assertTrue(daySteps(readings, later, zone) > 0L)
        assertEquals(15, october.size)
        assertEquals(today, october.last())
        assertEquals(todaySteps, october.sumOf { daySteps(readings, it, zone) })
        assertEquals(todaySteps, bars.last().steps)
        assertTrue(bars.last().steps < todaySteps + daySteps(readings, tomorrow, zone) + daySteps(readings, later, zone))
    }

    private fun windowDays(): List<LocalDate> {
        val days = ArrayList<LocalDate>()
        var day = LocalDate.of(2025, 11, 1)
        while (!day.isAfter(today)) {
            days += day
            day = day.plusDays(1)
        }
        return days
    }

    private fun stepsIn(bars: List<TrendMonthBar>, month: YearMonth): Long =
        bars.single { YearMonth.from(it.lastDay) == month }.steps

    private fun reading(cumulative: Long, instant: String) =
        StepReading(cumulative, Instant.parse(instant))
}
