package io.github.enterenn.steply.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeakTrendDayTest {
    private val today = LocalDate.of(2026, 10, 15)

    @Test
    fun allZeroWindowHasNoRecord() {
        val bars = List(7) { index -> bar(daysAgo = 6 - index, steps = 0) }

        assertNull(peakTrendDay(bars))
    }

    @Test
    fun oneDayWins() {
        val bars = listOf(
            bar(6, 100),
            bar(5, 0),
            bar(4, 4_000),
            bar(1, 200),
            bar(0, 50),
        )

        assertEquals(bar(4, 4_000), peakTrendDay(bars))
    }

    @Test
    fun tieKeepsTheMostRecentDay() {
        val bars = listOf(
            bar(5, 800),
            bar(3, 100),
            bar(1, 800),
        )

        assertEquals(bar(1, 800), peakTrendDay(bars))
    }

    @Test
    fun sevenDayPeakDiffersFromThirtyDayPeak() {
        val thirty = List(30) { index ->
            val daysAgo = 29 - index
            val steps = when (daysAgo) {
                20 -> 9_000L
                2 -> 3_000L
                else -> 10L
            }
            bar(daysAgo, steps)
        }
        val seven = thirty.takeLast(7)

        assertEquals(today.minusDays(20), peakTrendDay(thirty)?.day)
        assertEquals(today.minusDays(2), peakTrendDay(seven)?.day)
    }

    private fun bar(daysAgo: Int, steps: Long) =
        TrendDayBar(today.minusDays(daysAgo.toLong()), steps)
}
