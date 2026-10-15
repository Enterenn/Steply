package io.github.enterenn.steply.ui.menu

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.enterenn.steply.R
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.theme.SteplyTheme

private val PhosphorRegular = FontFamily(Font(R.font.phosphor_regular))
private const val ArrowLeftGlyph = "\uE058"
private const val CaretRightGlyph = "\uE13A"
private val BackTarget = 48.dp
private const val FaqMotionMs = 200

private val Faq = listOf(
    R.string.help_storage_q to R.string.help_storage_a,
    R.string.help_permission_q to R.string.help_permission_a,
    R.string.help_background_q to R.string.help_background_a,
    R.string.help_goal_q to R.string.help_goal_a,
    R.string.help_estimates_q to R.string.help_estimates_a,
    R.string.help_units_q to R.string.help_units_a,
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val spacing = SteplyTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .submenuPadding(),
        verticalArrangement = Arrangement.spacedBy(spacing.stack),
    ) {
        HelpTitle(onBack)
        Faq.forEach { (question, answer) ->
            FaqCard(question, answer)
        }
    }
}

@Composable
private fun HelpTitle(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteplyTheme.spacing.inline),
    ) {
        HelpBackButton(onClick = onBack)
        BasicText(
            text = stringResource(R.string.menu_help),
            modifier = Modifier.semantics { heading() },
            style = SteplyTheme.typography.body.copy(color = SteplyTheme.colors.gray),
        )
    }
}

@Composable
private fun FaqCard(@StringRes question: Int, @StringRes answer: Int) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val spacing = SteplyTheme.spacing
    val state = stringResource(
        if (expanded) R.string.help_expanded_a11y else R.string.help_collapsed_a11y,
    )
    val interaction = remember { MutableInteractionSource() }
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(FaqMotionMs, easing = EaseInOutCubic),
        label = "faq",
    )
    var answerHeight by remember { mutableIntStateOf(0) }
    val reveal = with(LocalDensity.current) { (answerHeight * progress).toDp() }
    SteplyCard(contentPadding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BackTarget)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = { expanded = !expanded },
                )
                .semantics(mergeDescendants = true) {
                    heading()
                    stateDescription = state
                }
                .padding(horizontal = spacing.card),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = stringResource(question),
                modifier = Modifier.weight(1f),
                style = SteplyTheme.typography.body.copy(
                    color = SteplyTheme.colors.text,
                    fontWeight = FontWeight.W600,
                ),
            )
            BasicText(
                text = CaretRightGlyph,
                modifier = Modifier
                    .padding(start = spacing.inline)
                    .rotate(90f - 180f * progress)
                    .clearAndSetSemantics {},
                style = TextStyle(
                    fontFamily = PhosphorRegular,
                    fontSize = SteplyTheme.typography.body.fontSize,
                    color = SteplyTheme.colors.gray,
                ),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(reveal)
                .clipToBounds(),
        ) {
            BasicText(
                text = stringResource(answer),
                modifier = Modifier
                    .wrapContentHeight(unbounded = true)
                    .onSizeChanged { answerHeight = it.height }
                    .graphicsLayer { alpha = progress }
                    .then(if (expanded) Modifier else Modifier.clearAndSetSemantics {})
                    .padding(
                        start = spacing.card,
                        top = spacing.card,
                        end = spacing.card,
                        bottom = spacing.card,
                    ),
                style = SteplyTheme.typography.body.copy(color = SteplyTheme.colors.gray),
            )
        }
    }
}

@Composable
private fun HelpBackButton(onClick: () -> Unit) {
    val label = stringResource(R.string.settings_back)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(BackTarget)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = ArrowLeftGlyph,
            modifier = Modifier.clearAndSetSemantics {},
            style = TextStyle(
                fontFamily = PhosphorRegular,
                fontSize = SteplyTheme.typography.body.fontSize,
                color = SteplyTheme.colors.gray,
            ),
        )
    }
}
