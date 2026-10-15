package io.github.enterenn.steply.data.steps

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.enterenn.steply.R
import io.github.enterenn.steply.ui.theme.AccentPreset
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

private val DistanceKey = stringPreferencesKey("distance_unit")
private val WeightKey = stringPreferencesKey("weight_unit")
private val HeightKey = stringPreferencesKey("height_unit")
private val GoalNotificationsKey = booleanPreferencesKey("goal_notifications")
private val ThemeKey = stringPreferencesKey("theme_mode")
private val AccentKey = stringPreferencesKey("accent")
private val LanguageKey = stringPreferencesKey("language")
private val GoalNotifiedOnKey = stringPreferencesKey("goal_notified_on")
private val LastOptimizationKey = stringPreferencesKey("last_optimization")

private const val KilometersPerMile = 1.609344
private const val PoundsPerKilogram = 2.2046226218
private const val CentimetersPerInch = 2.54
private const val GoalChannelId = "goal"
private const val GoalNoticeId = 1

enum class DistanceUnit { Km, Mi }
enum class WeightUnit { Kg, Lb }
enum class HeightUnit { Cm, In }
enum class ThemeMode { System, Light, Dark }
enum class AppLanguage { System, English, French }

data class Settings(
    val distance: DistanceUnit = DistanceUnit.Km,
    val weight: WeightUnit = WeightUnit.Kg,
    val height: HeightUnit = HeightUnit.Cm,
    val goalNotifications: Boolean = false,
    val theme: ThemeMode = ThemeMode.System,
    val accent: AccentPreset = AccentPreset.Blue,
    val language: AppLanguage = AppLanguage.System,
    val goalNotifiedOn: LocalDate? = null,
)

fun kmToMi(kilometers: Double): Double = kilometers / KilometersPerMile

fun kgToLb(kg: Int): Int = (kg * PoundsPerKilogram).roundToInt()
    .coerceIn(kgToLbRaw(ProfileWeightMinKg), kgToLbRaw(ProfileWeightMaxKg))

fun lbToKg(lb: Int): Int = (lb / PoundsPerKilogram).roundToInt()
    .coerceIn(ProfileWeightMinKg, ProfileWeightMaxKg)

fun cmToIn(cm: Int): Int = (cm / CentimetersPerInch).roundToInt()
    .coerceIn(cmToInRaw(ProfileHeightMinCm), cmToInRaw(ProfileHeightMaxCm))

fun inchesToCm(inches: Int): Int = (inches * CentimetersPerInch).roundToInt()
    .coerceIn(ProfileHeightMinCm, ProfileHeightMaxCm)

class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<Settings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::settingsFrom)

    suspend fun snapshot(): Settings = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .first()
        .let(::settingsFrom)

    suspend fun setDistance(unit: DistanceUnit) {
        dataStore.edit { it[DistanceKey] = if (unit == DistanceUnit.Mi) "mi" else "km" }
    }

    suspend fun setWeight(unit: WeightUnit) {
        dataStore.edit { it[WeightKey] = if (unit == WeightUnit.Lb) "lb" else "kg" }
    }

    suspend fun setHeight(unit: HeightUnit) {
        dataStore.edit { it[HeightKey] = if (unit == HeightUnit.In) "in" else "cm" }
    }

    suspend fun setGoalNotifications(enabled: Boolean) {
        dataStore.edit { it[GoalNotificationsKey] = enabled }
    }

    suspend fun setTheme(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[ThemeKey] = when (mode) {
                ThemeMode.Light -> "light"
                ThemeMode.Dark -> "dark"
                ThemeMode.System -> "system"
            }
        }
    }

    suspend fun setAccent(preset: AccentPreset) {
        dataStore.edit { it[AccentKey] = preset.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            when (language) {
                AppLanguage.System -> prefs.remove(LanguageKey)
                AppLanguage.English -> prefs[LanguageKey] = "en"
                AppLanguage.French -> prefs[LanguageKey] = "fr"
            }
        }
    }

    suspend fun setGoalNotifiedOn(day: LocalDate) {
        dataStore.edit { it[GoalNotifiedOnKey] = day.toString() }
    }

    suspend fun lastOptimization(): String? = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .first()[LastOptimizationKey]
        ?.takeIf { raw ->
            try {
                Instant.parse(raw)
                true
            } catch (_: DateTimeParseException) {
                false
            }
        }

    suspend fun setLastOptimization(instant: Instant) {
        dataStore.edit { it[LastOptimizationKey] = instant.toString() }
    }
}

fun Context.withStoredLanguage(): Context {
    val language = try {
        runBlocking { applicationContext.settingsDataStore.data.first() }.let(::settingsFrom).language
    } catch (error: IOException) {
        AppLanguage.System
    }
    return withLanguage(language)
}

fun Context.withLanguage(language: AppLanguage): Context {
    val tag = when (language) {
        AppLanguage.System -> return this
        AppLanguage.English -> "en"
        AppLanguage.French -> "fr"
    }
    val locale = Locale.forLanguageTag(tag)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return createConfigurationContext(config)
}

fun canPostGoalNotice(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

fun postGoalNotice(context: Context, language: AppLanguage): Boolean {
    if (!canPostGoalNotice(context)) return false
    val localized = context.withLanguage(language)
    val manager = context.getSystemService(NotificationManager::class.java) ?: return false
    manager.createNotificationChannel(
        NotificationChannel(
            GoalChannelId,
            localized.getString(R.string.settings_goal_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ),
    )
    val notification = Notification.Builder(localized, GoalChannelId)
        .setSmallIcon(R.mipmap.ic_launcher_foreground)
        .setContentTitle(localized.getString(R.string.settings_goal_notice_title))
        .setContentText(localized.getString(R.string.settings_goal_notice_body))
        .setAutoCancel(true)
        .build()
    manager.notify(GoalNoticeId, notification)
    return true
}

private fun kgToLbRaw(kg: Int): Int = (kg * PoundsPerKilogram).roundToInt()

private fun cmToInRaw(cm: Int): Int = (cm / CentimetersPerInch).roundToInt()

private fun settingsFrom(preferences: Preferences): Settings {
    val accent = preferences[AccentKey]?.let { name ->
        AccentPreset.entries.firstOrNull { it.name == name }
    } ?: AccentPreset.Blue
    return Settings(
        distance = if (preferences[DistanceKey] == "mi") DistanceUnit.Mi else DistanceUnit.Km,
        weight = if (preferences[WeightKey] == "lb") WeightUnit.Lb else WeightUnit.Kg,
        height = if (preferences[HeightKey] == "in") HeightUnit.In else HeightUnit.Cm,
        goalNotifications = preferences[GoalNotificationsKey] == true,
        theme = when (preferences[ThemeKey]) {
            "light" -> ThemeMode.Light
            "dark" -> ThemeMode.Dark
            else -> ThemeMode.System
        },
        accent = accent,
        language = when (preferences[LanguageKey]) {
            "en" -> AppLanguage.English
            "fr" -> AppLanguage.French
            else -> AppLanguage.System
        },
        goalNotifiedOn = preferences[GoalNotifiedOnKey]?.let { raw ->
            try {
                LocalDate.parse(raw)
            } catch (error: DateTimeParseException) {
                null
            }
        },
    )
}
