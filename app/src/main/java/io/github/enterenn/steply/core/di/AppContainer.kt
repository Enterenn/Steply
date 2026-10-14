package io.github.enterenn.steply.core.di

import android.content.Context
import androidx.room.Room
import io.github.enterenn.steply.data.db.AppDatabase

/** Injection manuelle : point unique de construction des dépendances. */
class AppContainer(context: Context) {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, AppDatabase.NAME)
            .build()
    }
}
