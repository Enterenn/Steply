package io.github.enterenn.steply.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.enterenn.steply.ui.theme.SteplyTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val TouchTarget = 48.dp
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private const val PressedScale = 0.94f
private const val OvershootScale = 1.03f
private const val RestScale = 1f

enum class SteplyPillButtonStyle {
    Primary,
    Secondary,
    DestructiveText,
}

@Composable
fun SteplyCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val padding = contentPadding ?: PaddingValues(SteplyTheme.spacing.card)
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(min = TouchTarget)
            .clip(RoundedCornerShape(SteplyTheme.radius.card))
            .background(SteplyTheme.colors.card)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SteplyListRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = SteplyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    var rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = TouchTarget)
        .semantics(mergeDescendants = true) {}
    if (onClick != null) {
        rowModifier = rowModifier.clickable(
            interactionSource = interaction,
            indication = null,
            role = Role.Button,
            onClick = onClick,
        )
    }
    Row(
        modifier = rowModifier.padding(horizontal = SteplyTheme.spacing.card),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = SteplyTheme.typography.body.copy(color = colors.text),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (value != null) {
            BasicText(
                text = value,
                modifier = Modifier.padding(start = SteplyTheme.spacing.inline),
                style = SteplyTheme.typography.caption.copy(color = colors.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SteplySegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SteplyTheme.spacing
    val colors = SteplyTheme.colors
    val index = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    var trackWidth by remember { mutableIntStateOf(0) }
    var thumbPlaced by remember { mutableStateOf(false) }
    val thumbOffset = remember { Animatable(0f) }
    val segmentWidth = if (options.isEmpty() || trackWidth == 0) 0 else trackWidth / options.size
    LaunchedEffect(index, segmentWidth) {
        if (segmentWidth == 0) return@LaunchedEffect
        val target = (segmentWidth * index).toFloat()
        if (!thumbPlaced) {
            thumbOffset.snapTo(target)
            thumbPlaced = true
        } else {
            thumbOffset.animateTo(target, tween(250, easing = EaseInOutCubic))
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteplyTheme.radius.button))
            .background(colors.lightGray)
            .padding(spacing.inline / 2),
    ) {
        Box(Modifier.matchParentSize().onSizeChanged { trackWidth = it.width }) {
            if (segmentWidth > 0) {
                Box(
                    Modifier
                        .offset { IntOffset(thumbOffset.value.roundToInt(), 0) }
                        .width(with(LocalDensity.current) { segmentWidth.toDp() })
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(SteplyTheme.radius.button))
                        .background(colors.card),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { optionIndex, option ->
                Segment(
                    label = option,
                    selected = optionIndex == index,
                    onClick = { onSelect(optionIndex) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.Segment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = TouchTarget)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = SteplyTheme.typography.body.copy(
                color = SteplyTheme.colors.text,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun Modifier.pressBounce(): Modifier = composed {
    val scale = remember { Animatable(RestScale) }
    val scope = rememberCoroutineScope()
    val motion = remember { mutableStateOf<Job?>(null) }
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            var pressed = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val down = event.changes.any { it.pressed }
                if (!pressed && event.changes.any { it.changedToDownIgnoreConsumed() }) {
                    pressed = true
                    motion.value?.cancel()
                    motion.value = scope.launch {
                        scale.animateTo(PressedScale, tween(80, easing = EaseOutCubic))
                    }
                } else if (pressed && !down) {
                    pressed = false
                    motion.value?.cancel()
                    motion.value = scope.launch {
                        scale.animateTo(OvershootScale, tween(180, easing = EaseOutCubic))
                        scale.animateTo(RestScale, tween(220, easing = EaseOutCubic))
                    }
                }
            }
        }
    }
}

@Composable
fun SteplyPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SteplyPillButtonStyle = SteplyPillButtonStyle.Primary,
) {
    val colors = SteplyTheme.colors
    val background = when (style) {
        SteplyPillButtonStyle.Primary -> colors.accentFill
        SteplyPillButtonStyle.Secondary -> colors.lightGray
        SteplyPillButtonStyle.DestructiveText -> null
    }
    val contentColor = when (style) {
        SteplyPillButtonStyle.Primary -> colors.onAccentFill
        SteplyPillButtonStyle.Secondary -> colors.text
        SteplyPillButtonStyle.DestructiveText -> colors.error
    }
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressBounce()
            .widthIn(min = TouchTarget)
            .heightIn(min = TouchTarget)
            .clip(RoundedCornerShape(SteplyTheme.radius.button))
            .then(if (background != null) Modifier.background(background) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = SteplyTheme.spacing.card),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = SteplyTheme.typography.body.copy(color = contentColor),
        )
    }
}

@Composable
fun SteplySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = SteplyTheme.colors
    val spacing = SteplyTheme.spacing
    val interaction = remember { MutableInteractionSource() }
    val thumbSize = spacing.card
    val progress = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) {
        progress.animateTo(if (checked) 1f else 0f, tween(200, easing = EaseInOutCubic))
    }
    val travel = TouchTarget - spacing.inline - thumbSize
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = TouchTarget)
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = SteplyTheme.typography.body.copy(color = colors.text),
            modifier = Modifier.weight(1f).padding(end = spacing.inline),
        )
        Box(
            Modifier
                .width(TouchTarget)
                .height(thumbSize + spacing.inline)
                .clip(RoundedCornerShape(SteplyTheme.radius.button))
                .background(lerp(colors.lightGray, colors.accentFill, progress.value))
                .padding(spacing.inline / 2),
        ) {
            Box(
                Modifier
                    .offset(x = travel * progress.value)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(colors.onAccentFill),
            )
        }
    }
}
