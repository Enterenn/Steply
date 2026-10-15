package io.github.enterenn.steply.data.steps

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyGoalTest {
    @Test
    fun defaultsToEightThousand() {
        assertEquals(8_000, resolveDailyGoal(null))
    }

    @Test
    fun keepsValueAlreadyInRange() {
        assertEquals(12_500, resolveDailyGoal(12_500))
    }

    @Test
    fun snapsOutOfRangeToNearestStep() {
        assertEquals(1_000, resolveDailyGoal(0))
        assertEquals(30_000, resolveDailyGoal(40_000))
    }

    @Test
    fun snapsToFiveHundredStep() {
        assertEquals(8_000, resolveDailyGoal(8_200))
        assertEquals(8_500, resolveDailyGoal(8_300))
    }
}
