package io.github.enterenn.steply

import android.app.Application
import io.github.enterenn.steply.core.di.AppContainer

class SteplyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
