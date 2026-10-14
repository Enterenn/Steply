package io.github.enterenn.steply.core.di

import android.content.Context
import androidx.room.Room
import io.github.enterenn.steply.core.hasActivityRecognitionPermission
import io.github.enterenn.steply.data.db.AppDatabase
import io.github.enterenn.steply.data.db.MIGRATION_1_2
import io.github.enterenn.steply.data.steps.StepCounterReader
import io.github.enterenn.steply.data.steps.StepCounterRepository
import io.github.enterenn.steply.data.steps.StepReadingsState
import io.github.enterenn.steply.data.steps.enqueueStepCounterWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _activityRecognitionGranted =
        MutableStateFlow(appContext.hasActivityRecognitionPermission())
    val activityRecognitionGranted: StateFlow<Boolean> = _activityRecognitionGranted.asStateFlow()

    val stepReadings: StateFlow<StepReadingsState>
        get() = stepCounterRepository.state

    var activityRecognitionPromptShown: Boolean = false

    init {
        scope.launch { stepCounterRepository.refreshFromDatabase() }
    }

    fun onAppForeground() {
        val granted = appContext.hasActivityRecognitionPermission()
        _activityRecognitionGranted.value = granted
        if (!granted) return
        enqueueStepCounterWork(appContext)
        scope.launch { stepCounterRepository.recordReading() }
    }
}
