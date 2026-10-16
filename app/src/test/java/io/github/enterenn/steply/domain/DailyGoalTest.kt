package io.github.enterenn.steply.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun snapsToOneHundredStep() {
        assertEquals(1_100, resolveDailyGoal(1_100))
        assertEquals(1_200, resolveDailyGoal(1_249))
        assertEquals(1_300, resolveDailyGoal(1_250))
    }

    @Test
    fun decreaseAtOrAboveTodayStepsIsAccepted() {
        assertTrue(acceptsDailyGoalChange(10_000, 8_000, 8_000L))
        assertTrue(acceptsDailyGoalChange(10_000, 8_000, 7_000L))
    }

    @Test
    fun decreaseBelowTodayStepsIsRefused() {
        assertFalse(acceptsDailyGoalChange(10_000, 5_000, 8_000L))
    }

    @Test
    fun increaseIsAcceptedEvenBelowTodaySteps() {
        assertTrue(acceptsDailyGoalChange(4_000, 6_000, 8_000L))
    }

    @Test
    fun decreaseIsAcceptedWhenTodayHasNoSteps() {
        assertTrue(acceptsDailyGoalChange(10_000, 1_000, 0L))
    }
}
