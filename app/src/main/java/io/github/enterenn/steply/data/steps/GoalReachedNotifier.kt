package io.github.enterenn.steply.data.steps

import io.github.enterenn.steply.domain.AppLanguage
import io.github.enterenn.steply.domain.shouldPostGoalNotice
import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Poster de la notification « objectif atteint ». La prod délègue à [canPostGoalNotice] et [postGoalNotice]. */
interface GoalNoticePoster {
    fun canPost(): Boolean
    fun post(language: AppLanguage): Boolean
}

/**
 * Au plus une notification par jour civil local quand les pas stockés atteignent l'objectif.
 * Ne calcule pas les pas : sous le verrou, il relit le jour, les pas, l'objectif et les réglages.
 *
 * [evaluate] est appelé par :
 * - [StepCollectionCoordinator] après une collecte (worker, live via le callback du repository, lecture de premier plan) ;
 * - le premier plan, une fois, si la permission activité manque (pas de lecture) ;
 * - l'activation des notifications d'objectif ;
 * - un changement d'objectif ;
 * - un import CSV accepté.
 */
class GoalReachedNotifier(
    private val today: () -> LocalDate,
    private val stepsOn: suspend (LocalDate) -> Long,
    private val goal: suspend () -> Int,
    private val settings: suspend () -> Settings,
    private val markNotified: suspend (LocalDate) -> Unit,
    private val poster: GoalNoticePoster,
) {
    private val lock = Mutex()

    suspend fun evaluate() = lock.withLock {
        val day = today()
        val fresh = settings()
        val stepsToday = stepsOn(day)
        val currentGoal = goal()
        if (!shouldPostGoalNotice(
                stepsToday = stepsToday,
                goal = currentGoal,
                notificationsEnabled = fresh.goalNotifications,
                notifiedOn = fresh.goalNotifiedOn,
                today = day,
            )
        ) return@withLock
        if (!poster.canPost()) return@withLock
        if (poster.post(fresh.language)) markNotified(day)
    }
}
