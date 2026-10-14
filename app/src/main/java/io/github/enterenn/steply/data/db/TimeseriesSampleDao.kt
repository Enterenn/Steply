package io.github.enterenn.steply.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TimeseriesSampleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(samples: List<TimeseriesSampleEntity>)

    @Query("SELECT COUNT(*) FROM timeseries_samples")
    suspend fun count(): Long
}
