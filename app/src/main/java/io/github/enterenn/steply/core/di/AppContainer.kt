package io.github.enterenn.steply.core.di

import android.content.Context
import androidx.room.Room
import io.github.enterenn.steply.core.AppLifecycleHandler
import io.github.enterenn.steply.core.hasActivityRecognitionPermission
import io.github.enterenn.steply.data.db.AppDatabase
import io.github.enterenn.steply.data.db.MIGRATION_1_2
import io.github.enterenn.steply.domain.AppLanguage
import io.github.enterenn.steply.domain.DailyGoalHistory
import io.github.enterenn.steply.data.steps.DailyGoalStore
import io.github.enterenn.steply.domain.DistanceUnit
import io.github.enterenn.steply.domain.HeightUnit
import io.github.enterenn.steply.data.steps.OnboardingStore
import io.github.enterenn.steply.domain.Profile
import io.github.enterenn.steply.data.steps.ProfileStore
import io.github.enterenn.steply.data.steps.Settings
import io.github.enterenn.steply.data.steps.SettingsStore
import io.github.enterenn.steply.data.steps.StepCollectionCoordinator
import io.github.enterenn.steply.data.steps.StepCounterReader
import io.github.enterenn.steply.data.steps.StepCounterRepository
import io.github.enterenn.steply.data.steps.StepReadingsState
import io.github.enterenn.steply.domain.ThemeMode
import io.github.enterenn.steply.domain.WeightUnit
import io.github.enterenn.steply.data.steps.AndroidGoalNoticePoster
import io.github.enterenn.steply.data.steps.GoalReachedNotifier
import io.github.enterenn.steply.data.steps.enqueueStepCounterWork
import io.github.enterenn.steply.ui.theme.AccentPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
            database = database,
            settings = settingsStore,
        )
    }

    val stepCollectionCoordinator: StepCollectionCoordinator by lazy {
        StepCollectionCoordinator(
            context = appContext,
            repository = stepCounterRepository,
            goalReachedNotifier = goalReachedNotifier,
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
            goalReachedNotifier.evaluate()
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
    val goalReachedNotifier: GoalReachedNotifier by lazy {
        GoalReachedNotifier(
            today = { LocalDate.now() },
            stepsOn = { day -> stepCounterRepository.stepsOn(day) },
            goal = { dailyGoalStore.goal.first() },
            settings = { settingsStore.snapshot() },
            markNotified = { day -> settingsStore.setGoalNotifiedOn(day) },
            poster = AndroidGoalNoticePoster(appContext),
        )
    }
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
        scope.launch {
            settingsStore.setGoalNotifications(enabled)
            if (enabled) goalReachedNotifier.evaluate()
        }
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
    }

    private val lifecycleHandler = AppLifecycleHandler(
        hasPermission = { stepCollectionCoordinator.hasActivityRecognitionPermission() },
        publishGranted = { granted -> _activityRecognitionGranted.value = granted },
        stopLive = { stepCounterRepository.stopLiveUpdates() },
        startLive = {
            stepCounterRepository.startLiveUpdates(scope) { cumulative ->
                stepCollectionCoordinator.recordCumulative(cumulative)
            }
        },
        enqueue = { enqueueStepCounterWork(appContext) },
        recordReading = { scope.launch { stepCollectionCoordinator.recordReading() } },
        evaluate = { scope.launch { goalReachedNotifier.evaluate() } },
    )

    fun onAppForeground() {
        lifecycleHandler.onForeground()
    }

    fun onAppBackground() {
        lifecycleHandler.onBackground()
    }
}
