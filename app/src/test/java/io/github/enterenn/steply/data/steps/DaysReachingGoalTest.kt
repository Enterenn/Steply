package io.github.enterenn.steply.data.steps

import org.junit.Assert.assertEquals
import org.junit.Test

class DaysReachingGoalTest {
    @Test
    fun emptyListReturnsZero() {
        assertEquals(0, daysReachingGoal(emptyList(), 8_000))
    }

    @Test
    fun noDayReachesGoal() {
        assertEquals(0, daysReachingGoal(listOf(0L, 1_000L, 7_999L), 8_000))
    }

    @Test
    fun severalDays() {
        assertEquals(3, daysReachingGoal(listOf(8_000L, 0L, 9_000L, 8_001L, 100L), 8_000))
    }

    @Test
    fun equalToGoal() {
        assertEquals(1, daysReachingGoal(listOf(8_000L), 8_000))
    }

    @Test
    fun aboveGoal() {
        assertEquals(2, daysReachingGoal(listOf(8_001L, 12_000L), 8_000))
    }

    @Test
    fun negativeSteps() {
        assertEquals(0, daysReachingGoal(listOf(-1L, -10_000L), 8_000))
        assertEquals(1, daysReachingGoal(listOf(-1L, 8_000L), 8_000))
    }
}
