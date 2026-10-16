package io.github.enterenn.steply.data.steps

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.enterenn.steply.domain.Profile
import io.github.enterenn.steply.domain.resolveProfile
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "profile")

private val NameKey = stringPreferencesKey("profile_display_name")
private val WeightKey = intPreferencesKey("profile_weight_kg")
private val HeightKey = intPreferencesKey("profile_height_cm")

class ProfileStore(context: Context) {
    private val dataStore = context.applicationContext.profileDataStore

    val profile: Flow<Profile> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> readProfile(preferences) }

    suspend fun setName(raw: String) {
        dataStore.edit { preferences ->
            val name = resolveProfile(readProfile(preferences), proposedName = raw).displayName
            if (name == null) preferences.remove(NameKey) else preferences[NameKey] = name
        }
    }

    suspend fun setWeightKg(kg: Int) {
        dataStore.edit { preferences ->
            val weight = resolveProfile(readProfile(preferences), proposedWeightKg = kg).weightKg
            if (weight == null) preferences.remove(WeightKey) else preferences[WeightKey] = weight
        }
    }

    suspend fun setHeightCm(cm: Int) {
        dataStore.edit { preferences ->
            val height = resolveProfile(readProfile(preferences), proposedHeightCm = cm).heightCm
            if (height == null) preferences.remove(HeightKey) else preferences[HeightKey] = height
        }
    }
}

private fun readProfile(preferences: Preferences): Profile = resolveProfile(
    proposedName = preferences[NameKey],
    proposedWeightKg = preferences[WeightKey],
    proposedHeightCm = preferences[HeightKey],
)
