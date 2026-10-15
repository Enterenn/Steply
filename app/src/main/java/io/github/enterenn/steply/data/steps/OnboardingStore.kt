package io.github.enterenn.steply.data.steps

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

private val CompleteKey = booleanPreferencesKey("onboarding_complete")

class OnboardingStore(context: Context) {
    private val dataStore = context.applicationContext.onboardingDataStore

    val complete: Flow<Boolean> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[CompleteKey] == true }

    suspend fun completeOnboarding() {
        dataStore.edit { preferences ->
            preferences[CompleteKey] = true
        }
    }
}
