package io.github.enterenn.steply.data.steps

import io.github.enterenn.steply.domain.AppLanguage
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalReachedNotifierTest {
    private val today = LocalDate.of(2026, 10, 16)
    private var steps = 0L
    private var goal = 8_000
    private var settings = Settings(goalNotifications = true, language = AppLanguage.French)
    private val marked = mutableListOf<LocalDate>()
    private val poster = FakePoster()
    private val notifier = GoalReachedNotifier(
        today = { today },
        stepsOn = { day ->
            check(day == today)
            steps
        },
        goal = { goal },
        settings = { settings },
        markNotified = { day ->
            marked += day
            settings = settings.copy(goalNotifiedOn = day)
        },
        poster = poster,
    )

    @Test
    fun staysSilentBelowGoal() {
        steps = 7_999
        evaluate()
        assertEquals(0, poster.posts)
        assertTrue(marked.isEmpty())
    }

    @Test
    fun postsAndMarksTheDayWhenGoalIsReached() {
        steps = 8_000
        evaluate()
        assertEquals(1, poster.posts)
        assertEquals(AppLanguage.French, poster.language)
        assertEquals(listOf(today), marked)
    }

    @Test
    fun staysSilentWhenAlreadyNotifiedToday() {
        steps = 9_000
        settings = settings.copy(goalNotifiedOn = today)
        evaluate()
        assertEquals(0, poster.posts)
        assertTrue(marked.isEmpty())
    }

    @Test
    fun staysSilentWhenNotificationsAreOff() {
        steps = 9_000
        settings = settings.copy(goalNotifications = false)
        evaluate()
        assertEquals(0, poster.posts)
        assertTrue(marked.isEmpty())
    }

    @Test
    fun doesNotPostAgainWhenGoalChangesAfterANoticeTheSameDay() {
        steps = 8_000
        evaluate()
        goal = 4_000
        evaluate()
        assertEquals(1, poster.posts)
        assertEquals(listOf(today), marked)
    }

    @Test
    fun staysSilentWhenGoalIsNotPositive() {
        steps = 100
        goal = 0
        evaluate()
        goal = -1
        evaluate()
        assertEquals(0, poster.posts)
        assertTrue(marked.isEmpty())
    }

    @Test
    fun staysSilentWhenPostingIsNotAllowed() {
        steps = 9_000
        poster.allowed = false
        evaluate()
        assertEquals(0, poster.posts)
        assertTrue(marked.isEmpty())
        assertNull(settings.goalNotifiedOn)
    }

    @Test
    fun doesNotMarkTheDayWhenThePosterFails() {
        steps = 9_000
        poster.posted = false
        evaluate()
        assertEquals(1, poster.posts)
        assertTrue(marked.isEmpty())
        assertNull(settings.goalNotifiedOn)
    }

    private fun evaluate() = runBlocking { notifier.evaluate() }

    private class FakePoster : GoalNoticePoster {
        var allowed = true
        var posted = true
        var posts = 0
        var language: AppLanguage? = null

        override fun canPost(): Boolean = allowed

        override fun post(language: AppLanguage): Boolean {
            posts += 1
            this.language = language
            return posted
        }
    }
}
