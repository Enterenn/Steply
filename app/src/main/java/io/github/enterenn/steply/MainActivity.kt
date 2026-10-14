package io.github.enterenn.steply

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.enterenn.steply.core.di.AppContainer
import io.github.enterenn.steply.ui.SteplyApp
import io.github.enterenn.steply.ui.theme.SteplyTheme

class MainActivity : ComponentActivity() {
    private val container: AppContainer
        get() = (application as SteplyApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SteplyTheme {
                SteplyApp(container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.onAppForeground()
    }
}
