package io.github.enterenn.steply.data.steps

import java.time.LocalDate

data class DailyGoalChange(
    val day: LocalDate,
    val goal: Int,
)

data class DailyGoalHistory(
    val startingGoal: Int? = null,
    val changes: List<DailyGoalChange> = emptyList(),
)

/** Dernière prise d'effet à cette date ou avant ; sinon l'objectif de départ. */
fun goalsForDays(
    days: List<LocalDate>,
    startingGoal: Int,
    changes: List<DailyGoalChange>,
): List<Int> {
    if (days.isEmpty()) return emptyList()
    val ordered = changes
        .withIndex()
        .sortedWith(compareBy({ it.value.day }, { it.index }))
        .map { it.value }
    var cursor = 0
    var current = startingGoal
    return days.map { day ->
        while (cursor < ordered.size && !ordered[cursor].day.isAfter(day)) {
            current = ordered[cursor].goal
            cursor++
        }
        current
    }
}
