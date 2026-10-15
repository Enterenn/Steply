package io.github.enterenn.steply.data.steps

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyGoalHistoryTest {
    private val start = LocalDate.of(2026, 10, 10)

    @Test
    fun noHistoryUsesStartingGoal() {
        val days = days(7)
        assertEquals(List(7) { 8_000 }, goalsForDays(days, 8_000, emptyList()))
    }

    @Test
    fun changeInTheMiddleOfThePeriod() {
        val days = days(7)
        val changes = listOf(DailyGoalChange(start.plusDays(3), 10_000))
        assertEquals(
            listOf(8_000, 8_000, 8_000, 10_000, 10_000, 10_000, 10_000),
            goalsForDays(days, 8_000, changes),
        )
    }

    @Test
    fun changeAfterThePeriodKeepsStartingGoal() {
        val days = days(7)
        val changes = listOf(DailyGoalChange(start.plusDays(20), 10_000))
        assertEquals(List(7) { 8_000 }, goalsForDays(days, 8_000, changes))
    }

    @Test
    fun changeBeforeThePeriodAppliesToEveryDay() {
        val days = days(7)
        val changes = listOf(DailyGoalChange(start.minusDays(9), 10_000))
        assertEquals(List(7) { 10_000 }, goalsForDays(days, 8_000, changes))
    }

    @Test
    fun twoChangesOnTheSameDayKeepTheLast() {
        val days = days(7)
        val day = start.plusDays(3)
        val changes = listOf(
            DailyGoalChange(day, 10_000),
            DailyGoalChange(day, 12_000),
        )
        assertEquals(
            listOf(8_000, 8_000, 8_000, 12_000, 12_000, 12_000, 12_000),
            goalsForDays(days, 8_000, changes),
        )
    }

    @Test
    fun emptyDayListReturnsNoGoals() {
        val changes = listOf(DailyGoalChange(start, 10_000))
        assertEquals(emptyList<Int>(), goalsForDays(emptyList(), 8_000, changes))
    }

    private fun days(count: Int) = List(count) { index -> start.plusDays(index.toLong()) }
}
