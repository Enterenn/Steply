package io.github.enterenn.steply.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Relevé brut du compteur cumulatif matériel (jalon M1).
 * `timeseries_samples` décrit un intervalle agrégé : ce cumulatif n'y a pas sa place.
 */
@Entity(tableName = "step_counter_readings")
data class StepCounterReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "cumulative_steps") val cumulativeSteps: Long,
    @ColumnInfo(name = "recorded_at") val recordedAt: String,
    @ColumnInfo(name = "zone_offset") val zoneOffset: String,
)
