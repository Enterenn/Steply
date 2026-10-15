package io.github.enterenn.steply.data.steps

import kotlin.math.roundToInt

private const val DefaultStrideMeters = 0.76
private const val DefaultWeightKg = 70.0
private const val StrideFactor = 0.414

data class DerivedActivity(
    val kilocalories: Int,
    val distanceKm: Double,
    val walkingMinutes: Double,
)

fun derivedActivity(
    steps: Long,
    heightCm: Double? = null,
    weightKg: Double? = null,
): DerivedActivity {
    val safeSteps = steps.coerceAtLeast(0L)
    val minutes = safeSteps / 100.0
    val stride = if (heightCm == null) DefaultStrideMeters else heightCm / 100.0 * StrideFactor
    val weight = weightKg ?: DefaultWeightKg
    val kcal = (3.5 * 3.5 * weight / 200.0 * minutes).roundToInt()
    return DerivedActivity(
        kilocalories = kcal,
        distanceKm = safeSteps * stride / 1_000.0,
        walkingMinutes = minutes,
    )
}
