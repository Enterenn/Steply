package io.github.enterenn.steply.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSteplyColors = staticCompositionLocalOf { steplyColors(AccentPreset.Blue, dark = false) }
private val LocalSteplyTypography = staticCompositionLocalOf { steplyTypography() }

object SteplyTheme {
    val colors: SteplyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSteplyColors.current

    val typography: SteplyTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalSteplyTypography.current
}

@Composable
fun SteplyTheme(
    accent: AccentPreset = AccentPreset.Blue,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSteplyColors provides steplyColors(accent, dark),
        LocalSteplyTypography provides steplyTypography(),
        content = content,
    )
}
