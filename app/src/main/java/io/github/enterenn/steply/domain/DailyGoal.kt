package io.github.enterenn.steply.domain

import kotlin.math.roundToInt

const val DailyGoalDefault = 8_000
const val DailyGoalMin = 1_000
const val DailyGoalMax = 30_000
const val DailyGoalStep = 500
const val DailyGoalMajorEvery = 1_000

/** Absent value stays at the default. Anything else snaps to the nearest step in range. */
fun resolveDailyGoal(stored: Int?): Int {
    val raw = stored ?: return DailyGoalDefault
    val clamped = raw.coerceIn(DailyGoalMin, DailyGoalMax)
    val index = ((clamped - DailyGoalMin).toFloat() / DailyGoalStep).roundToInt()
    return (DailyGoalMin + index * DailyGoalStep).coerceIn(DailyGoalMin, DailyGoalMax)
}

/** Une baisse sous les pas déjà faits aujourd'hui est refusée. */
fun acceptsDailyGoalChange(current: Int, proposed: Int, stepsToday: Long): Boolean {
    if (proposed >= current) return true
    return proposed.toLong() >= stepsToday.coerceAtLeast(0L)
}
