package io.github.enterenn.steply.core.di

import android.content.Context
import androidx.room.Room
import io.github.enterenn.steply.core.hasActivityRecognitionPermission
import io.github.enterenn.steply.data.db.AppDatabase
import io.github.enterenn.steply.data.db.MIGRATION_1_2
import io.github.enterenn.steply.data.steps.DailyGoalHistory
import io.github.enterenn.steply.data.steps.DailyGoalStore
import io.github.enterenn.steply.data.steps.OnboardingStore
import io.github.enterenn.steply.data.steps.Profile
import io.github.enterenn.steply.data.steps.ProfileStore
import io.github.enterenn.steply.data.steps.StepCounterReader
import io.github.enterenn.steply.data.steps.StepCounterRepository
import io.github.enterenn.steply.data.steps.StepReadingsState
import io.github.enterenn.steply.data.steps.enqueueStepCounterWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Injection manuelle : point unique de construction des dépendances. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    val stepCounterRepository: StepCounterRepository by lazy {
        StepCounterRepository(
            dao = database.stepCounterReadingDao(),
            reader = StepCounterReader(appContext),
        )
    }

    private val dailyGoalStore: DailyGoalStore by lazy { DailyGoalStore(appContext) }
    val dailyGoal: Flow<Int>
        get() = dailyGoalStore.goal

    val dailyGoalHistory: Flow<DailyGoalHistory>
        get() = dailyGoalStore.history

    fun setDailyGoal(steps: Int) {
        scope.launch {
            val walkedToday = stepCounterRepository.stepsOn(LocalDate.now())
            dailyGoalStore.setGoal(steps, walkedToday)
        }
    }

    private val profileStore: ProfileStore by lazy { ProfileStore(appContext) }
    val profile: Flow<Profile>
        get() = profileStore.profile

    fun setProfileName(raw: String) {
        scope.launch { profileStore.setName(raw) }
    }

    fun setProfileWeightKg(kg: Int) {
        scope.launch { profileStore.setWeightKg(kg) }
    }

    fun setProfileHeightCm(cm: Int) {
        scope.launch { profileStore.setHeightCm(cm) }
    }

    private val onboardingStore: OnboardingStore by lazy { OnboardingStore(appContext) }
    private val _onboardingComplete = MutableStateFlow<Boolean?>(null)
    val onboardingComplete: StateFlow<Boolean?> = _onboardingComplete.asStateFlow()

    fun completeOnboarding() {
        _onboardingComplete.value = true
        scope.launch { onboardingStore.completeOnboarding() }
    }

    private val _activityRecognitionGranted =
        MutableStateFlow(appContext.hasActivityRecognitionPermission())
    val activityRecognitionGranted: StateFlow<Boolean> = _activityRecognitionGranted.asStateFlow()

    val stepReadings: StateFlow<StepReadingsState>
        get() = stepCounterRepository.state

    init {
        scope.launch { stepCounterRepository.refreshFromDatabase() }
        scope.launch {
            onboardingStore.complete.collect { done ->
                if (done) _onboardingComplete.value = true
                else if (_onboardingComplete.value != true) _onboardingComplete.value = false
            }
        }
    }

    fun onAppForeground() {
        val granted = appContext.hasActivityRecognitionPermission()
        _activityRecognitionGranted.value = granted
        if (!granted) {
            stepCounterRepository.stopLiveUpdates()
            return
        }
        enqueueStepCounterWork(appContext)
        stepCounterRepository.startLiveUpdates(scope)
        scope.launch { stepCounterRepository.recordReading() }
    }

    fun onAppBackground() {
        stepCounterRepository.stopLiveUpdates()
    }
}
