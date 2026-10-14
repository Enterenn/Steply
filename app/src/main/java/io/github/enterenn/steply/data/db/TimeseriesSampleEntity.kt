package io.github.enterenn.steply.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Un échantillon = un intervalle agrégé. Horodatages en UTC (ISO 8601), `zoneOffset` immuable. */
@Entity(
    tableName = "timeseries_samples",
    indices = [
        Index(
            value = ["type", "resolution", "provider", "device_id", "start_time"],
            unique = true,
        ),
    ],
)
data class TimeseriesSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val unit: String,
    val resolution: String,
    val provider: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "start_time") val startTime: String,
    @ColumnInfo(name = "end_time") val endTime: String,
    @ColumnInfo(name = "zone_offset") val zoneOffset: String,
    val value: Double,
)
