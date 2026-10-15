package io.github.enterenn.steply.core.di

import android.content.Context
import androidx.room.Room
import io.github.enterenn.steply.core.hasActivityRecognitionPermission
import io.github.enterenn.steply.data.db.AppDatabase
import io.github.enterenn.steply.data.db.MIGRATION_1_2
import io.github.enterenn.steply.data.steps.AppLanguage
import io.github.enterenn.steply.data.steps.DailyGoalHistory
import io.github.enterenn.steply.data.steps.DailyGoalStore
import io.github.enterenn.steply.data.steps.DistanceUnit
import io.github.enterenn.steply.data.steps.HeightUnit
import io.github.enterenn.steply.data.steps.OnboardingStore
import io.github.enterenn.steply.data.steps.Profile
import io.github.enterenn.steply.data.steps.ProfileStore
import io.github.enterenn.steply.data.steps.Settings
import io.github.enterenn.steply.data.steps.SettingsStore
import io.github.enterenn.steply.data.steps.StepCounterReader
import io.github.enterenn.steply.data.steps.StepCounterRepository
import io.github.enterenn.steply.data.steps.StepReadingsState
import io.github.enterenn.steply.data.steps.ThemeMode
import io.github.enterenn.steply.data.steps.WeightUnit
import io.github.enterenn.steply.data.steps.canPostGoalNotice
import io.github.enterenn.steply.data.steps.enqueueStepCounterWork
import io.github.enterenn.steply.data.steps.postGoalNotice
import io.github.enterenn.steply.ui.theme.AccentPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

    private val settingsStore: SettingsStore by lazy { SettingsStore(appContext) }
    private val goalNotice = Mutex()
    val settings: Flow<Settings>
        get() = settingsStore.settings

    suspend fun currentSettings(): Settings = settingsStore.snapshot()

    fun setDistanceUnit(unit: DistanceUnit) {
        scope.launch { settingsStore.setDistance(unit) }
    }

    fun setWeightUnit(unit: WeightUnit) {
        scope.launch { settingsStore.setWeight(unit) }
    }

    fun setHeightUnit(unit: HeightUnit) {
        scope.launch { settingsStore.setHeight(unit) }
    }

    fun setGoalNotifications(enabled: Boolean) {
        scope.launch { settingsStore.setGoalNotifications(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        scope.launch { settingsStore.setTheme(mode) }
    }

    fun setAccent(preset: AccentPreset) {
        scope.launch { settingsStore.setAccent(preset) }
    }

    fun setLanguage(language: AppLanguage, onApplied: () -> Unit = {}) {
        scope.launch {
            settingsStore.setLanguage(language)
            withContext(Dispatchers.Main) { onApplied() }
        }
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
        scope.launch {
            combine(
                settingsStore.settings,
                dailyGoalStore.goal,
                stepCounterRepository.state,
            ) { settings, goal, _ -> settings to goal }
                .collect { (settings, goal) -> considerGoalNotice(settings, goal) }
        }
    }

    private suspend fun considerGoalNotice(settings: Settings, goal: Int) {
        if (!settings.goalNotifications) return
        val today = LocalDate.now()
        if (settings.goalNotifiedOn == today) return
        if (!canPostGoalNotice(appContext)) return
        goalNotice.withLock {
            val day = LocalDate.now()
            val steps = stepCounterRepository.stepsOn(day)
            if (steps < goal.toLong()) return@withLock
            val fresh = settingsStore.snapshot()
            if (!fresh.goalNotifications || fresh.goalNotifiedOn == day) return@withLock
            if (!canPostGoalNotice(appContext)) return@withLock
            if (postGoalNotice(appContext, fresh.language)) settingsStore.setGoalNotifiedOn(day)
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
