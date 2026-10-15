package io.github.enterenn.steply.data.steps

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Retire des relevés pour alléger l'historique. Ne décale aucun cumul.
 * Moins de 48 heures : tous. De 48 heures à 30 jours : le dernier de chaque heure locale.
 * Au-delà de 30 jours : le premier et le dernier de chaque jour local.
 * Le plus ancien, le plus récent et les deux bords d'une remise à zéro restent.
 * Un relevé de plus reste si le retirer change les pas d'un jour.
 * Les relevés sont dans l'ordre d'enregistrement.
 */
fun compactReadings(readings: List<StepReading>, zone: ZoneId, now: Instant): List<StepReading> {
    val count = readings.size
    if (count < 2) return readings
    val keep = BooleanArray(count)
    keep[0] = true
    keep[count - 1] = true
    markResets(readings, keep)
    markWindows(readings, zone, now, keep)
    preserveDaySteps(readings, zone, keep)
    return readings.filterIndexed { index, _ -> keep[index] }
}

private fun markResets(readings: List<StepReading>, keep: BooleanArray) {
    for (index in 1 until readings.size) {
        val delta = stepDelta(readings[index - 1].cumulativeSteps, readings[index].cumulativeSteps) ?: continue
        if (!delta.counterReset) continue
        keep[index - 1] = true
        keep[index] = true
    }
}

private fun markWindows(
    readings: List<StepReading>,
    zone: ZoneId,
    now: Instant,
    keep: BooleanArray,
) {
    val zonedNow = now.atZone(zone)
    val recentStart = zonedNow.minusHours(48).toInstant()
    val monthStart = zonedNow.minusDays(30).toInstant()
    val hourLast = HashMap<Instant, Int>()
    val dayFirst = HashMap<LocalDate, Int>()
    val dayLast = HashMap<LocalDate, Int>()
    for (index in readings.indices) {
        val time = readings[index].recordedAt
        if (time.isAfter(recentStart)) {
            keep[index] = true
            continue
        }
        val zoned = time.atZone(zone)
        if (time.isBefore(monthStart)) {
            val day = zoned.toLocalDate()
            val first = dayFirst[day]
            if (first == null || index < first) dayFirst[day] = index
            val last = dayLast[day]
            if (last == null || index > last) dayLast[day] = index
        } else {
            val hour = zoned.truncatedTo(ChronoUnit.HOURS).toInstant()
            val previous = hourLast[hour]
            if (previous == null || index > previous) hourLast[hour] = index
        }
    }
    for (index in dayFirst.values) keep[index] = true
    for (index in dayLast.values) keep[index] = true
    for (index in hourLast.values) keep[index] = true
}

private fun preserveDaySteps(readings: List<StepReading>, zone: ZoneId, keep: BooleanArray) {
    val anchors = ArrayList<Int>()
    for (index in readings.indices) {
        if (keep[index]) anchors.add(index)
    }
    var cursor = 0
    while (cursor < anchors.size - 1) {
        val start = anchors[cursor]
        val end = anchors[cursor + 1]
        if (end == start + 1 || gapPreservesDaySteps(readings, start, end, zone)) {
            cursor++
            continue
        }
        val mid = (start + end) / 2
        keep[mid] = true
        anchors.add(cursor + 1, mid)
    }
}

private fun gapPreservesDaySteps(
    readings: List<StepReading>,
    start: Int,
    end: Int,
    zone: ZoneId,
): Boolean {
    var firstDay = readings[start].recordedAt.atZone(zone).toLocalDate()
    var lastDay = firstDay
    for (index in start + 1..end) {
        val day = readings[index].recordedAt.atZone(zone).toLocalDate()
        if (day.isBefore(firstDay)) firstDay = day
        if (day.isAfter(lastDay)) lastDay = day
    }
    if (firstDay == lastDay) return true
    val full = readings.subList(start, end + 1)
    val reduced = listOf(readings[start], readings[end])
    var day = firstDay
    while (!day.isAfter(lastDay)) {
        if (daySteps(full, day, zone) != daySteps(reduced, day, zone)) return false
        day = day.plusDays(1)
    }
    return true
}
