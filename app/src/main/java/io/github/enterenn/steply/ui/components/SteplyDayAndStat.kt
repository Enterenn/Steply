package io.github.enterenn.steply.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.enterenn.steply.R
import io.github.enterenn.steply.ui.theme.SteplyTheme

private val DayPillMinHeight = 64.dp
private val DayLabelSize = 10.sp
private val DayLabelLineHeight = 12.sp
private val DayLabelGap = 0.dp
private val DayNumberSize = 20.sp
private val DayNumberLineHeight = 22.sp
private val StatIconSize = 32.sp
private val StatValueSize = 28.sp
private val StatValueLineHeight = 32.sp

private val PhosphorRegular = FontFamily(Font(R.font.phosphor_regular))

@Composable
fun SteplyDayPill(
    abbreviation: String,
    dayNumber: String,
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = SteplyTheme.colors
    val spacing = SteplyTheme.spacing
    val background = if (selected) colors.accentFill else colors.lightGray
    val labelColor = if (selected) colors.onAccentFill else colors.gray
    val numberColor = if (selected) colors.onAccentFill else colors.text
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .pressBounce()
            .fillMaxWidth()
            .heightIn(min = DayPillMinHeight)
            .clip(RoundedCornerShape(SteplyTheme.radius.button))
            .background(background)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = label
            }
            .padding(vertical = spacing.inline),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DayLabelGap, Alignment.CenterVertically),
    ) {
        BasicText(
            text = abbreviation,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            style = SteplyTheme.typography.caption.copy(
                color = labelColor,
                fontSize = DayLabelSize,
                lineHeight = DayLabelLineHeight,
                fontWeight = FontWeight.W600,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            softWrap = false,
        )
        BasicText(
            text = dayNumber,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            style = SteplyTheme.typography.display.copy(
                color = numberColor,
                fontSize = DayNumberSize,
                lineHeight = DayNumberLineHeight,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
fun SteplyStatTile(
    glyph: String,
    value: String,
    unit: String,
    caption: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = SteplyTheme.colors
    val spacing = SteplyTheme.spacing
    val interaction = remember { MutableInteractionSource() }
    var tileModifier = modifier.semantics(mergeDescendants = true) {}
    if (onClick != null) {
        tileModifier = modifier
            .pressBounce()
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {}
    }
    SteplyCard(modifier = tileModifier) {
        BasicText(
            text = glyph,
            modifier = Modifier.clearAndSetSemantics {},
            style = TextStyle(
                fontFamily = PhosphorRegular,
                fontSize = StatIconSize,
                color = colors.accent,
            ),
        )
        Row(
            modifier = Modifier
                .padding(top = spacing.inline)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicText(
                text = value,
                modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
                style = SteplyTheme.typography.display.copy(
                    color = colors.text,
                    fontSize = StatValueSize,
                    lineHeight = StatValueLineHeight,
                    fontWeight = FontWeight.W700,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = unit,
                modifier = Modifier
                    .padding(start = spacing.inline / 2)
                    .alignByBaseline(),
                style = SteplyTheme.typography.caption.copy(color = colors.text),
                maxLines = 1,
            )
        }
        BasicText(
            text = caption,
            modifier = Modifier.padding(top = spacing.inline),
            style = SteplyTheme.typography.caption.copy(color = colors.text),
        )
    }
}
