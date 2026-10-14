package io.github.enterenn.steply.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `step_counter_readings` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cumulative_steps` INTEGER NOT NULL, " +
                "`recorded_at` TEXT NOT NULL, " +
                "`zone_offset` TEXT NOT NULL)",
        )
    }
}
