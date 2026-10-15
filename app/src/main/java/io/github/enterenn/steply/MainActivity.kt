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
import io.github.enterenn.steply.data.steps.ThemeMode
import io.github.enterenn.steply.data.steps.withStoredLanguage
import io.github.enterenn.steply.ui.SteplyApp
import io.github.enterenn.steply.ui.theme.SteplyTheme

class MainActivity : ComponentActivity() {
    private val container: AppContainer
        get() = (application as SteplyApplication).container

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withStoredLanguage())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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
}
