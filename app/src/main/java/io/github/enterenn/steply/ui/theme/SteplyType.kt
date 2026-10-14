package io.github.enterenn.steply.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.enterenn.steply.R

/** Both variable fonts default to weight 300, so each entry pins the wght axis. */
private val VariableWeights = listOf(
    FontWeight.W300,
    FontWeight.W400,
    FontWeight.W500,
    FontWeight.W600,
    FontWeight.W700,
    FontWeight.W800,
    FontWeight.W900,
)

@OptIn(ExperimentalTextApi::class)
private fun variableFont(
    resId: Int,
    weight: FontWeight,
    style: FontStyle = FontStyle.Normal,
) = Font(
    resId = resId,
    weight = weight,
    style = style,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val SteplyUiFont = FontFamily(
    VariableWeights.flatMap { weight ->
        listOf(
            variableFont(R.font.figtree_variable, weight),
            variableFont(R.font.figtree_italic_variable, weight, FontStyle.Italic),
        )
    },
)

val SteplyFiguresFont = FontFamily(
    VariableWeights.map { weight -> variableFont(R.font.darker_grotesque_variable, weight) },
)
