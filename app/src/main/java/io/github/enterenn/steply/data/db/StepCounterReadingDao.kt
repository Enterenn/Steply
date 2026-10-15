package io.github.enterenn.steply.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface StepCounterReadingDao {
    @Insert
    suspend fun insert(reading: StepCounterReadingEntity)

    @Insert
    suspend fun insertAll(readings: List<StepCounterReadingEntity>)

    @Query("SELECT * FROM step_counter_readings ORDER BY id DESC LIMIT 2")
    suspend fun latestTwo(): List<StepCounterReadingEntity>

    @Query("SELECT * FROM step_counter_readings ORDER BY id ASC")
    suspend fun allById(): List<StepCounterReadingEntity>

    @Query("SELECT COUNT(*) FROM step_counter_readings")
    suspend fun count(): Long

    @Query("DELETE FROM step_counter_readings")
    suspend fun deleteAll()

    @Query("DELETE FROM timeseries_samples")
    suspend fun deleteTimeseriesSamples()

    @Transaction
    suspend fun replaceAll(readings: List<StepCounterReadingEntity>) {
        deleteAll()
        if (readings.isNotEmpty()) insertAll(readings)
    }

    @Transaction
    suspend fun purgeSteps() {
        deleteAll()
        deleteTimeseriesSamples()
    }
}
