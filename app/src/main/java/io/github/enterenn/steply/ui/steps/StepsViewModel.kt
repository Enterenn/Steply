package io.github.enterenn.steply.ui.steps

import androidx.lifecycle.ViewModel
import io.github.enterenn.steply.data.steps.Settings
import io.github.enterenn.steply.data.steps.StepReadingsState
import io.github.enterenn.steply.domain.DailyGoalDefault
import io.github.enterenn.steply.domain.DistanceUnit
import io.github.enterenn.steply.domain.Profile
import io.github.enterenn.steply.domain.shouldCelebrateGoal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

data class StepsUiState(
    val activityRecognitionGranted: Boolean,
    val readings: StepReadingsState,
    val goal: Int,
    val heightCm: Int?,
    val weightKg: Int?,
    val distance: DistanceUnit,
    val steps: Long,
    val weekSteps: List<Long>,
    val celebrationShownOn: LocalDate? = null,
    val settingsReady: Boolean = false,
    val showCelebration: Boolean = false,
)

class StepsViewModel(
    activityRecognitionGranted: StateFlow<Boolean>,
    stepReadings: StateFlow<StepReadingsState>,
    private val dailyGoal: Flow<Int>,
    private val profile: Flow<Profile>,
    private val settings: Flow<Settings>,
    private val stepsOnDay: suspend (LocalDate) -> Long,
    private val onSetDailyGoal: (Int) -> Unit,
    private val onMarkCelebration: (LocalDate) -> Unit,
) : ViewModel() {
    private var celebrationArmedFor: LocalDate? = null
    private val granted = activityRecognitionGranted
    private val readingsFlow = stepReadings
    private val _uiState = MutableStateFlow(
        StepsUiState(
            activityRecognitionGranted = activityRecognitionGranted.value,
            readings = stepReadings.value,
            goal = DailyGoalDefault,
            heightCm = Profile().heightCm,
            weightKg = Profile().weightKg,
            distance = Settings().distance,
            steps = 0L,
            weekSteps = List(WeekDayCount) { 0L },
        ),
    )
    val uiState: StateFlow<StepsUiState> = _uiState.asStateFlow()

    suspend fun observeInputs() {
        combine(
            granted,
            readingsFlow,
            dailyGoal,
            profile,
            settings,
        ) { grantedNow, readings, goal, profileNow, settingsNow ->
            Inputs(
                granted = grantedNow,
                readings = readings,
                goal = goal,
                heightCm = profileNow.heightCm,
                weightKg = profileNow.weightKg,
                distance = settingsNow.distance,
                celebrationShownOn = settingsNow.celebrationShownOn,
            )
        }.collect { inputs ->
            _uiState.update { current ->
                current.copy(
                    activityRecognitionGranted = inputs.granted,
                    readings = inputs.readings,
                    goal = inputs.goal,
                    heightCm = inputs.heightCm,
                    weightKg = inputs.weightKg,
                    distance = inputs.distance,
                    celebrationShownOn = inputs.celebrationShownOn,
                    settingsReady = true,
                )
            }
        }
    }

    suspend fun loadSelectedDay(
        date: LocalDate,
        today: LocalDate = date,
        goal: Int = _uiState.value.goal,
        shownOn: LocalDate? = _uiState.value.celebrationShownOn,
        celebrate: Boolean = false,
    ) {
        val steps = stepsOnDay(date)
        val due = celebrate &&
            date == today &&
            shouldCelebrateGoal(steps, goal, shownOn, today)
        val start = due && celebrationArmedFor != today
        if (start) {
            celebrationArmedFor = today
            onMarkCelebration(today)
        }
        _uiState.update { state ->
            val show = when {
                date != today -> false
                start -> true
                else -> state.showCelebration && date == today
            }
            state.copy(
                steps = steps,
                showCelebration = show,
                celebrationShownOn = if (start) today else state.celebrationShownOn,
            )
        }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(showCelebration = false) }
    }

    suspend fun loadWeek(dates: List<LocalDate>) {
        val counts = dates.map { day -> stepsOnDay(day) }
        _uiState.update { it.copy(weekSteps = counts) }
    }

    suspend fun stepsOn(day: LocalDate): Long = stepsOnDay(day)

    fun setDailyGoal(value: Int) {
        onSetDailyGoal(value)
    }

    private data class Inputs(
        val granted: Boolean,
        val readings: StepReadingsState,
        val goal: Int,
        val heightCm: Int?,
        val weightKg: Int?,
        val distance: DistanceUnit,
        val celebrationShownOn: LocalDate?,
    )

    private companion object {
        const val WeekDayCount = 7
    }
}
