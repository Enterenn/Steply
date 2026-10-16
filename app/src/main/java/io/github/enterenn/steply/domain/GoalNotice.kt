package io.github.enterenn.steply.domain

import java.time.LocalDate

/** Au plus une célébration par jour civil local, lorsque l'objectif du jour est atteint. */
fun shouldCelebrateGoal(
    stepsToday: Long,
    goal: Int,
    shownOn: LocalDate?,
    today: LocalDate,
): Boolean {
    if (goal <= 0 || shownOn == today) return false
    return stepsToday >= goal.toLong()
}

/** Au plus une notification par jour civil local, lorsque l'objectif du jour est atteint. */
fun shouldPostGoalNotice(
    stepsToday: Long,
    goal: Int,
    notificationsEnabled: Boolean,
    notifiedOn: LocalDate?,
    today: LocalDate,
): Boolean {
    if (!notificationsEnabled || goal <= 0 || notifiedOn == today) return false
    return stepsToday >= goal.toLong()
}
