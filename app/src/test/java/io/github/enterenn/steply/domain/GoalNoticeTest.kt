package io.github.enterenn.steply.domain

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalNoticeTest {
    private val today = LocalDate.of(2026, 10, 16)

    @Test
    fun celebratesWhenTheGoalIsFirstReachedToday() {
        assertTrue(shouldCelebrateGoal(8_000, 8_000, null, today))
        assertTrue(shouldCelebrateGoal(9_000, 8_000, today.minusDays(1), today))
    }

    @Test
    fun staysQuietWhenTheCelebrationAlreadyPlayedToday() {
        assertFalse(shouldCelebrateGoal(9_000, 8_000, today, today))
    }

    @Test
    fun staysQuietBelowTheGoalOrWithoutAGoal() {
        assertFalse(shouldCelebrateGoal(7_999, 8_000, null, today))
        assertFalse(shouldCelebrateGoal(100, 0, null, today))
    }

    @Test
    fun staysSilentBelowGoal() {
        assertFalse(shouldPostGoalNotice(7_999, 8_000, true, null, today))
    }

    @Test
    fun postsWhenGoalIsReached() {
        assertTrue(shouldPostGoalNotice(8_000, 8_000, true, null, today))
        assertTrue(shouldPostGoalNotice(9_000, 8_000, true, null, today))
    }

    @Test
    fun staysSilentWhenAlreadyNotifiedToday() {
        assertFalse(shouldPostGoalNotice(9_000, 8_000, true, today, today))
    }

    @Test
    fun staysSilentWhenNotificationsAreOff() {
        assertFalse(shouldPostGoalNotice(9_000, 8_000, false, null, today))
    }

    @Test
    fun staysSilentWhenGoalChangesAfterANoticeTheSameDay() {
        assertFalse(shouldPostGoalNotice(9_000, 4_000, true, today, today))
    }

    @Test
    fun staysSilentWhenGoalIsNotPositive() {
        assertFalse(shouldPostGoalNotice(100, 0, true, null, today))
        assertFalse(shouldPostGoalNotice(100, -1, true, null, today))
    }
}
