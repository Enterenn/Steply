package io.github.enterenn.steply.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface StepCounterReadingDao {
    @Insert
    suspend fun insert(reading: StepCounterReadingEntity)

    @Query("SELECT * FROM step_counter_readings ORDER BY id DESC LIMIT 2")
    suspend fun latestTwo(): List<StepCounterReadingEntity>

    @Query("SELECT * FROM step_counter_readings ORDER BY id ASC")
    suspend fun allById(): List<StepCounterReadingEntity>
}
