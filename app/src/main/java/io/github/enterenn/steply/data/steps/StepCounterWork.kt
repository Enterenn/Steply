package io.github.enterenn.steply.data.steps

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.enterenn.steply.SteplyApplication
import java.util.concurrent.TimeUnit

class StepCounterWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val coordinator = (applicationContext as SteplyApplication).container.stepCollectionCoordinator
        if (!coordinator.hasActivityRecognitionPermission()) return Result.success()
        coordinator.recordReading()
        coordinator.compactStoredReadings()
        return Result.success()
    }
}

fun enqueueStepCounterWork(context: Context) {
    val request = PeriodicWorkRequestBuilder<StepCounterWorker>(
        REPEAT_MINUTES,
        TimeUnit.MINUTES,
        FLEX_MINUTES,
        TimeUnit.MINUTES,
    ).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}

private const val UNIQUE_WORK_NAME = "step_counter_reading"
private const val REPEAT_MINUTES = 15L
private const val FLEX_MINUTES = 5L
