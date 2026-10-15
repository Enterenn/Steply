package io.github.enterenn.steply.data.steps

enum class CollectionHealth {
    Hidden,
    SensorMissing,
    TimedOut,
    NoReading,
}

fun collectionHealth(
    permissionGranted: Boolean,
    status: StepReadStatus,
    hasSnapshot: Boolean,
): CollectionHealth {
    if (!permissionGranted) return CollectionHealth.Hidden
    return when (status) {
        StepReadStatus.SensorMissing -> CollectionHealth.SensorMissing
        StepReadStatus.TimedOut -> CollectionHealth.TimedOut
        StepReadStatus.Idle -> if (hasSnapshot) CollectionHealth.Hidden else CollectionHealth.NoReading
    }
}
