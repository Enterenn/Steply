package io.github.enterenn.steply.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSteplyColors = staticCompositionLocalOf { steplyColors(AccentPreset.Blue, dark = false) }
private val LocalSteplyTypography = staticCompositionLocalOf { steplyTypography() }
private val LocalSteplySpacing = staticCompositionLocalOf { steplySpacing }
private val LocalSteplyRadius = staticCompositionLocalOf { steplyRadius }

object SteplyTheme {
    val colors: SteplyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSteplyColors.current

    val typography: SteplyTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalSteplyTypography.current

    val spacing: SteplySpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSteplySpacing.current

    val radius: SteplyRadius
        @Composable
        @ReadOnlyComposable
        get() = LocalSteplyRadius.current
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
        LocalSteplySpacing provides steplySpacing,
        LocalSteplyRadius provides steplyRadius,
        content = content,
    )
}
