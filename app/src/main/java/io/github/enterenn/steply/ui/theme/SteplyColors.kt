package io.github.enterenn.steply.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Couleurs sémantiques (§8.4) : `accent` pour les éléments graphiques,
 * `accentFill` + `onAccentFill` pour les fonds portant du texte.
 * Les valeurs viennent de [SteplyTokens].
 */
@Immutable
data class SteplyColors(
    val background: Color,
    val card: Color,
    val text: Color,
    val gray: Color,
    val lightGray: Color,
    val success: Color,
    val error: Color,
    val accent: Color,
    val accentFill: Color,
    val onAccentFill: Color,
)

private fun pick(dark: Boolean, light: Color, darkColor: Color): Color = if (dark) darkColor else light

fun steplyColors(accent: AccentPreset, dark: Boolean): SteplyColors = SteplyColors(
    background = pick(dark, SteplyTokens.backgroundLight, SteplyTokens.backgroundDark),
    card = pick(dark, SteplyTokens.cardLight, SteplyTokens.cardDark),
    text = pick(dark, SteplyTokens.textLight, SteplyTokens.textDark),
    gray = pick(dark, SteplyTokens.grayLight, SteplyTokens.grayDark),
    lightGray = pick(dark, SteplyTokens.lightGrayLight, SteplyTokens.lightGrayDark),
    success = pick(dark, SteplyTokens.successLight, SteplyTokens.successDark),
    error = pick(dark, SteplyTokens.errorLight, SteplyTokens.errorDark),
    accent = accentColor(accent, dark),
    accentFill = accentFillColor(accent, dark),
    onAccentFill = pick(dark, SteplyTokens.onAccentFillLight, SteplyTokens.onAccentFillDark),
)
