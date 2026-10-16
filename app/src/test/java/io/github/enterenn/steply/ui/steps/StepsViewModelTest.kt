package io.github.enterenn.steply.ui.steps

import io.github.enterenn.steply.data.steps.Settings
import io.github.enterenn.steply.data.steps.StepReadingsState
import io.github.enterenn.steply.domain.DailyGoalDefault
import io.github.enterenn.steply.domain.Profile
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class StepsViewModelTest {
    @Test
    fun loadsSelectedDay() = runBlocking {
        val requested = mutableListOf<LocalDate>()
        val day = LocalDate.of(2026, 10, 14)
        val viewModel = viewModel(
            stepsOn = { date ->
                requested += date
                1_200L
            },
        )
        viewModel.loadSelectedDay(day)
        assertEquals(1_200L, viewModel.uiState.value.steps)
        assertEquals(listOf(day), requested)
    }

    @Test
    fun reloadsWeek() = runBlocking {
        val requested = mutableListOf<LocalDate>()
        val days = List(7) { offset -> LocalDate.of(2026, 10, 12).plusDays(offset.toLong()) }
        var steps = 10L
        val viewModel = viewModel(
            stepsOn = { date ->
                requested += date
                steps
            },
        )
        viewModel.loadWeek(days)
        assertEquals(List(7) { 10L }, viewModel.uiState.value.weekSteps)
        steps = 25L
        viewModel.loadWeek(days)
        assertEquals(List(7) { 25L }, viewModel.uiState.value.weekSteps)
        assertEquals(days + days, requested)
    }

    @Test
    fun setDailyGoalDelegatesOnce() {
        val received = mutableListOf<Int>()
        val viewModel = viewModel(
            stepsOn = { error("stepsOn") },
            onSetDailyGoal = { received += it },
        )
        viewModel.setDailyGoal(8_500)
        assertEquals(listOf(8_500), received)
    }

    private fun viewModel(
        stepsOn: suspend (LocalDate) -> Long = { 0L },
        onSetDailyGoal: (Int) -> Unit = {},
    ) = StepsViewModel(
        activityRecognitionGranted = MutableStateFlow(false),
        stepReadings = MutableStateFlow(StepReadingsState()),
        dailyGoal = flowOf(DailyGoalDefault),
        profile = flowOf(Profile()),
        settings = flowOf(Settings()),
        stepsOnDay = stepsOn,
        onSetDailyGoal = onSetDailyGoal,
    )
}
