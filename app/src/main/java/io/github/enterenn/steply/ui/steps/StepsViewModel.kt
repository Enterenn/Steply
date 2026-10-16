package io.github.enterenn.steply.ui.steps

import androidx.lifecycle.ViewModel
import io.github.enterenn.steply.data.steps.Settings
import io.github.enterenn.steply.data.steps.StepReadingsState
import io.github.enterenn.steply.domain.DailyGoalDefault
import io.github.enterenn.steply.domain.DistanceUnit
import io.github.enterenn.steply.domain.Profile
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
)

class StepsViewModel(
    activityRecognitionGranted: StateFlow<Boolean>,
    stepReadings: StateFlow<StepReadingsState>,
    private val dailyGoal: Flow<Int>,
    private val profile: Flow<Profile>,
    private val settings: Flow<Settings>,
    private val stepsOnDay: suspend (LocalDate) -> Long,
    private val onSetDailyGoal: (Int) -> Unit,
) : ViewModel() {
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
                )
            }
        }
    }

    suspend fun loadSelectedDay(date: LocalDate) {
        val steps = stepsOnDay(date)
        _uiState.update { it.copy(steps = steps) }
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
    )

    private companion object {
        const val WeekDayCount = 7
    }
}
