package io.github.enterenn.steply.data.steps

import android.content.Context
import io.github.enterenn.steply.core.hasActivityRecognitionPermission

/**
 * Collecte explicite : permission activité, puis une lecture capteur ou un cumul déjà lu,
 * puis compaction des relevés stockés si l'appelant la demande. Ne recalcule rien.
 *
 * Le listener live, le debounce et la déduplication restent dans
 * [StepCounterRepository.startLiveUpdates]. Ce coordinateur sert seulement
 * les collectes déclenchées explicitement (worker périodique, retour au premier plan).
 */
class StepCollectionCoordinator(
    private val context: Context,
    private val repository: StepCounterRepository,
) {
    fun hasActivityRecognitionPermission(): Boolean = context.hasActivityRecognitionPermission()

    suspend fun recordReading() {
        repository.recordReading()
    }

    suspend fun recordCumulative(cumulativeSteps: Long) {
        repository.recordCumulative(cumulativeSteps)
    }

    suspend fun compactStoredReadings() {
        repository.compactStoredReadings()
    }
}
