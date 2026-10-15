package io.github.enterenn.steply.data.steps

import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class StepReading(
    val cumulativeSteps: Long,
    val recordedAt: Instant,
)

/**
 * Pas du jour local, calculés à la lecture des relevés bruts.
 * Un relevé sans précédent ne compte pas. Seule la part de l'intervalle qui tombe
 * dans le jour est gardée, au prorata entier depuis le début : le reste va à la fin,
 * donc deux jours voisins se recollent sans perte ni double comptage.
 * Les relevés sont dans l'ordre d'enregistrement.
 */
fun daySteps(readings: List<StepReading>, day: LocalDate, zone: ZoneId): Long {
    if (readings.size < 2) return 0L
    val dayStart = day.atStartOfDay(zone).toInstant()
    val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant()
    var total = 0L
    for (index in 1 until readings.size) {
        val previous = readings[index - 1]
        val current = readings[index]
        val delta = stepDelta(previous.cumulativeSteps, current.cumulativeSteps) ?: continue
        total += stepsDuring(delta.steps, previous.recordedAt, current.recordedAt, dayStart, dayEnd)
    }
    return total
}

private fun stepsDuring(
    steps: Long,
    start: Instant,
    end: Instant,
    dayStart: Instant,
    dayEnd: Instant,
): Long {
    if (steps == 0L) return 0L
    if (!end.isAfter(start)) {
        return if (!end.isBefore(dayStart) && end.isBefore(dayEnd)) steps else 0L
    }
    if (!end.isAfter(dayStart) || !start.isBefore(dayEnd)) return 0L
    val overlapStart = if (start.isAfter(dayStart)) start else dayStart
    val overlapEnd = if (end.isBefore(dayEnd)) end else dayEnd
    val durationNanos = Duration.between(start, end).toNanos()
    val allocatedEnd = portion(steps, Duration.between(start, overlapEnd).toNanos(), durationNanos)
    val allocatedStart = portion(steps, Duration.between(start, overlapStart).toNanos(), durationNanos)
    return allocatedEnd - allocatedStart
}

private fun portion(steps: Long, elapsedNanos: Long, durationNanos: Long): Long {
    if (steps == 0L || elapsedNanos <= 0L) return 0L
    if (elapsedNanos >= durationNanos) return steps
    return BigInteger.valueOf(steps)
        .multiply(BigInteger.valueOf(elapsedNanos))
        .divide(BigInteger.valueOf(durationNanos))
        .longValueExact()
}
