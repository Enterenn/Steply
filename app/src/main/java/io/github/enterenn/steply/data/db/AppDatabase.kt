package io.github.enterenn.steply.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TimeseriesSampleEntity::class, StepCounterReadingEntity::class],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeseriesSampleDao(): TimeseriesSampleDao
    abstract fun stepCounterReadingDao(): StepCounterReadingDao

    companion object {
        const val NAME = "steply.db"
    }
}
