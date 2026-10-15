package io.github.enterenn.steply.data.steps

import org.junit.Assert.assertEquals
import org.junit.Test

class DerivedActivityTest {
    @Test
    fun zeroSteps() {
        assertEquals(DerivedActivity(0, 0.0, 0.0), derivedActivity(0))
    }

    @Test
    fun negativeStepsBecomeZero() {
        assertEquals(derivedActivity(0), derivedActivity(-1_200))
    }

    @Test
    fun defaultsAreStrideAndWeight() {
        val metrics = derivedActivity(10_000)
        assertEquals(10_000 * 0.76 / 1_000.0, metrics.distanceKm, 0.0)
        assertEquals(429, metrics.kilocalories)
        assertEquals(metrics, derivedActivity(10_000, weightKg = 70.0))
    }

    @Test
    fun heightChangesDistance() {
        val base = derivedActivity(10_000)
        val taller = derivedActivity(10_000, heightCm = 180.0)
        assertEquals(10_000 * (180.0 / 100.0 * 0.414) / 1_000.0, taller.distanceKm, 0.0)
        assertEquals(base.kilocalories, taller.kilocalories)
        assertEquals(base.walkingMinutes, taller.walkingMinutes, 0.0)
    }

    @Test
    fun weightChangesKcalNotDistance() {
        val base = derivedActivity(10_000)
        val heavier = derivedActivity(10_000, weightKg = 80.0)
        assertEquals(490, heavier.kilocalories)
        assertEquals(base.distanceKm, heavier.distanceKm, 0.0)
        assertEquals(base.walkingMinutes, heavier.walkingMinutes, 0.0)
    }

    @Test
    fun durationIsStepsOverOneHundred() {
        assertEquals(0.0, derivedActivity(0).walkingMinutes, 0.0)
        assertEquals(2.5, derivedActivity(250).walkingMinutes, 0.0)
        assertEquals(100.0, derivedActivity(10_000).walkingMinutes, 0.0)
    }
}
