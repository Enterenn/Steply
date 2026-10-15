package io.github.enterenn.steply.data.steps

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dailyGoalDataStore by preferencesDataStore(name = "daily_goal")

class DailyGoalStore(context: Context) {
    private val dataStore = context.applicationContext.dailyGoalDataStore

    val goal: Flow<Int> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> resolveDailyGoal(preferences[GoalKey]) }

    suspend fun setGoal(steps: Int) {
        val resolved = resolveDailyGoal(steps)
        dataStore.edit { preferences -> preferences[GoalKey] = resolved }
    }

    private companion object {
        val GoalKey = intPreferencesKey("daily_goal_steps")
    }
}
