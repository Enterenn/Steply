package io.github.enterenn.steply.data.steps

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.enterenn.steply.domain.DailyGoalChange
import io.github.enterenn.steply.domain.DailyGoalHistory
import io.github.enterenn.steply.domain.acceptsDailyGoalChange
import io.github.enterenn.steply.domain.resolveDailyGoal
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dailyGoalDataStore by preferencesDataStore(name = "daily_goal")

class DailyGoalStore(context: Context, private val clock: Clock) {
    private val dataStore = context.applicationContext.dailyGoalDataStore

    val goal: Flow<Int> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> resolveDailyGoal(preferences[GoalKey]) }

    val history: Flow<DailyGoalHistory> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            DailyGoalHistory(
                startingGoal = preferences[StartKey],
                changes = decodeChanges(preferences[ChangesKey]),
            )
        }

    suspend fun setGoal(steps: Int, stepsToday: Long) {
        val resolved = resolveDailyGoal(steps)
        dataStore.edit { preferences ->
            val current = resolveDailyGoal(preferences[GoalKey])
            if (resolved == current) return@edit
            if (!acceptsDailyGoalChange(current, resolved, stepsToday)) return@edit
            // L'objectif d'avant le premier changement enregistré reste fixé.
            if (preferences[StartKey] == null) {
                preferences[StartKey] = current
            }
            val today = LocalDate.now(clock)
            preferences[ChangesKey] = withChange(preferences[ChangesKey], today, resolved)
            preferences[GoalKey] = resolved
        }
    }

    private companion object {
        val GoalKey = intPreferencesKey("daily_goal_steps")
        val StartKey = intPreferencesKey("daily_goal_start")
        val ChangesKey = stringPreferencesKey("daily_goal_changes")
    }
}

private fun withChange(encoded: String?, day: LocalDate, goal: Int): String {
    val kept = decodeChanges(encoded).filterNot { it.day == day }
    return (kept + DailyGoalChange(day, goal)).joinToString(";") { "${it.day}=${it.goal}" }
}

private fun decodeChanges(encoded: String?): List<DailyGoalChange> {
    if (encoded.isNullOrBlank()) return emptyList()
    return encoded.split(';').mapNotNull { token ->
        val parts = token.split('=', limit = 2)
        if (parts.size != 2) return@mapNotNull null
        val day = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
        val goal = parts[1].toIntOrNull() ?: return@mapNotNull null
        DailyGoalChange(day, goal)
    }
}
