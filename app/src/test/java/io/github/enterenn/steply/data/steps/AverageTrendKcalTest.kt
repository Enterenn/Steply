package io.github.enterenn.steply.data.steps

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AverageTrendKcalTest {
    @Test
    fun emptyListIsZero() {
        assertEquals(0, averageTrendKcal(emptyList()))
    }

    @Test
    fun oneDayIsThatDaysKcal() {
        assertEquals(429, averageTrendKcal(listOf(bar(10_000))))
    }

    @Test
    fun severalDaysAverageIntegerDailyKcal() {
        assertEquals(listOf(0, 429, 429), listOf(0L, 10_000L, 10_000L).map { derivedActivity(it).kilocalories })
        assertEquals(286, averageTrendKcal(listOf(bar(0), bar(10_000), bar(10_000))))
    }

    @Test
    fun roundsHalfUpLikeAverageTrendSteps() {
        assertEquals(214, derivedActivity(5_000).kilocalories)
        assertEquals(215, averageTrendKcal(listOf(bar(0), bar(10_000))))
    }

    private fun bar(steps: Long) = TrendDayBar(LocalDate.of(2026, 10, 15), steps)
}
