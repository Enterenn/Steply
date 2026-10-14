package io.github.enterenn.steply.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TimeseriesSampleEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeseriesSampleDao(): TimeseriesSampleDao

    companion object {
        const val NAME = "steply.db"
    }
}
