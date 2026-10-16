package io.github.enterenn.steply.data.steps

import android.content.Context
import io.github.enterenn.steply.core.hasActivityRecognitionPermission

/**
 * Collecte explicite : permission activité, puis une lecture capteur ou un cumul déjà lu,
 * puis compaction des relevés stockés si l'appelant la demande. Ne recalcule rien.
 *
 * Le listener live, le debounce et la déduplication restent dans
 * [StepCounterRepository.startLiveUpdates]. Son callback renvoie le cumul ici.
 * Après [recordReading] et [recordCumulative], l'objectif du jour est évalué.
 */
class StepCollectionCoordinator(
    private val context: Context,
    private val repository: StepCounterRepository,
    private val goalReachedNotifier: GoalReachedNotifier,
) {
    fun hasActivityRecognitionPermission(): Boolean = context.hasActivityRecognitionPermission()

    suspend fun recordReading() {
        repository.recordReading()
        goalReachedNotifier.evaluate()
    }

    suspend fun recordCumulative(cumulativeSteps: Long) {
        repository.recordCumulative(cumulativeSteps)
        goalReachedNotifier.evaluate()
    }

    suspend fun compactStoredReadings() {
        repository.compactStoredReadings()
    }
}
