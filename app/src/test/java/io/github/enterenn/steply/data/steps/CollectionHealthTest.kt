package io.github.enterenn.steply.data.steps

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionHealthTest {
    @Test
    fun permissionDeniedTakesPriority() {
        assertEquals(
            CollectionHealth.Hidden,
            collectionHealth(permissionGranted = false, StepReadStatus.SensorMissing, hasSnapshot = true),
        )
        assertEquals(
            CollectionHealth.Hidden,
            collectionHealth(permissionGranted = false, StepReadStatus.Idle, hasSnapshot = true),
        )
    }

    @Test
    fun sensorMissingEvenWithSnapshot() {
        assertEquals(
            CollectionHealth.SensorMissing,
            collectionHealth(permissionGranted = true, StepReadStatus.SensorMissing, hasSnapshot = true),
        )
    }

    @Test
    fun timedOutEvenWithSnapshot() {
        assertEquals(
            CollectionHealth.TimedOut,
            collectionHealth(permissionGranted = true, StepReadStatus.TimedOut, hasSnapshot = true),
        )
    }

    @Test
    fun noReadingWhenIdleWithoutSnapshot() {
        assertEquals(
            CollectionHealth.NoReading,
            collectionHealth(permissionGranted = true, StepReadStatus.Idle, hasSnapshot = false),
        )
    }

    @Test
    fun idleWithSnapshotStaysHidden() {
        assertEquals(
            CollectionHealth.Hidden,
            collectionHealth(permissionGranted = true, StepReadStatus.Idle, hasSnapshot = true),
        )
    }
}
