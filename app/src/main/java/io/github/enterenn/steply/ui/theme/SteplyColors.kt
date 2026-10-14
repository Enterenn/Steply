package io.github.enterenn.steply.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Teintes d'accent du cahier des charges (§8.3). */
enum class AccentPreset(val light: Color, val dark: Color) {
    Red(Color(0xFFE16969), Color(0xFF822828)),
    Orange(Color(0xFFEDA464), Color(0xFF8A4324)),
    Green(Color(0xFF71D086), Color(0xFF2E753E)),
    Blue(Color(0xFF6EBAFF), Color(0xFF296093)),
    Violet(Color(0xFF9384F8), Color(0xFF523C93)),
    Pink(Color(0xFFF693C2), Color(0xFF933C65)),
}

/**
 * Couleurs sémantiques (§8.4) : `accent` pour les éléments graphiques,
 * `accentFill` + `onAccentFill` pour les fonds portant du texte.
 */
@Immutable
data class SteplyColors(
    val background: Color,
    val card: Color,
    val text: Color,
    val accent: Color,
    val accentFill: Color,
    val onAccentFill: Color,
)

private val LightBackground = Color(0xFFF8F9FB)
private val LightCard = Color(0xFFFFFFFF)
private val LightText = Color(0xFF323337)
private val DarkBackground = Color(0xFF101115)
private val DarkCard = Color(0xFF1A1D26)
private val DarkText = Color(0xFFC8C8D7)

fun steplyColors(accent: AccentPreset, dark: Boolean): SteplyColors =
    if (dark) {
        SteplyColors(
            background = DarkBackground,
            card = DarkCard,
            text = DarkText,
            accent = accent.light,
            accentFill = accent.dark,
            onAccentFill = Color.White,
        )
    } else {
        SteplyColors(
            background = LightBackground,
            card = LightCard,
            text = LightText,
            accent = accent.light,
            accentFill = accent.light,
            onAccentFill = LightText,
        )
    }
