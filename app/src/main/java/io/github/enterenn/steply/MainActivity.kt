package io.github.enterenn.steply

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.enterenn.steply.core.di.AppContainer
import io.github.enterenn.steply.data.steps.Settings
import io.github.enterenn.steply.data.steps.withLanguage
import io.github.enterenn.steply.domain.AppLanguage
import io.github.enterenn.steply.domain.ThemeMode
import io.github.enterenn.steply.ui.SteplyApp
import io.github.enterenn.steply.ui.theme.SteplyTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private val container: AppContainer
        get() = (application as SteplyApplication).container
    private var attachedLanguage: AppLanguage = AppLanguage.System

    override fun attachBaseContext(newBase: Context) {
        val language = (newBase.applicationContext as SteplyApplication).container.language.value
        attachedLanguage = language
        super.attachBaseContext(newBase.withLanguage(language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(Unit) {
                val stored = container.settings.first().language
                if (container.language.value != stored) {
                    container.language.first { it == stored }
                }
                if (container.bootLanguageSuperseded || isFinishing) return@LaunchedEffect
                if (!appliedLanguageMatches(stored)) recreate()
            }
            val settings by container.settings.collectAsState(initial = Settings())
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings.theme) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { _ -> dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { _ -> dark },
                )
            }
            SteplyTheme(accent = settings.accent, dark = dark) {
                SteplyApp(container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.onAppForeground()
    }

    override fun onPause() {
        container.onAppBackground()
        super.onPause()
    }

    private fun appliedLanguageMatches(language: AppLanguage): Boolean {
        if (language == AppLanguage.System && attachedLanguage == AppLanguage.System) return true
        val applied = resources.configuration.locales[0].language
        val expected = when (language) {
            AppLanguage.English -> "en"
            AppLanguage.French -> "fr"
            AppLanguage.System -> applicationContext.resources.configuration.locales[0].language
        }
        return applied.equals(expected, ignoreCase = true)
    }
}
