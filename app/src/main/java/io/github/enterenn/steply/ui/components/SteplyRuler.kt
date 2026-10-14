package io.github.enterenn.steply.ui.components

import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.enterenn.steply.ui.theme.SteplyTheme
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive

private val ItemExtent = 10.dp
private val LabelRowHeight = 18.dp
private val MinorTickHeight = 12.dp
private val MajorTickHeight = 24.dp
private val TickStroke = 1.dp
private val CenterIndicatorWidth = 2.dp
private val CenterIndicatorHeight = 48.dp
private val SelectedValueGap = 4.dp
private val MajorLabelGap = 8.dp
private val ValueSize = 80.sp
private val ValueLineHeight = 88.sp
private val ReadoutHeight = 96.dp
private val UnitLabelGap = 16.dp
private val VerticalInset = 48.dp
private val TouchTarget = 48.dp
private const val SnapDurationMs = 200
private const val Friction = 0.088f
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

@Composable
fun SteplyRuler(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int,
    majorEvery: Int,
    unitLabel: String,
    contentDescription: String,
    inputContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val safeStep = step.coerceAtLeast(1)
    val safeMax = max.coerceAtLeast(min)
    val majorSteps = (majorEvery / safeStep).coerceAtLeast(1)
    val resolved = snapToStep(value, min, safeMax, safeStep)
    val lastIndex = (safeMax - min) / safeStep
    val colors = SteplyTheme.colors
    val density = LocalDensity.current
    val itemExtentPx = with(density) { ItemExtent.toPx() }
    val maxOffset = lastIndex * itemExtentPx
    val offsetState = remember(itemExtentPx) {
        mutableFloatStateOf(valueIndex(resolved, min, safeStep, lastIndex) * itemExtentPx)
    }
    val metrics = remember { RulerScrollMetrics() }
    val fieldEdit = remember { FieldEdit() }
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    metrics.itemExtentPx = itemExtentPx
    metrics.maxOffset = maxOffset
    metrics.lastIndex = lastIndex
    metrics.min = min
    metrics.step = safeStep
    metrics.value = resolved
    metrics.stopVelocity = ViewConfiguration.get(view.context).scaledMinimumFlingVelocity
        .toFloat()
        .coerceAtLeast(1f)
    metrics.onValueChange = onValueChange
    metrics.performTick = {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    val flingBehavior = remember(metrics, offsetState) {
        RulerFlingBehavior(offset = { offsetState.floatValue }, metrics = metrics)
    }
    val scrollable = rememberScrollableState { delta ->
        val extent = metrics.itemExtentPx
        val limit = metrics.maxOffset
        val current = offsetState.floatValue
        val next = (current + delta).coerceIn(0f, limit)
        val consumed = next - current
        if (consumed == 0f) return@rememberScrollableState 0f
        val before = indexForOffset(current, extent, metrics.lastIndex)
        offsetState.floatValue = next
        val after = indexForOffset(next, extent, metrics.lastIndex)
        if (after != before) {
            repeat(abs(after - before)) { metrics.performTick() }
            val nextValue = metrics.min + after * metrics.step
            if (nextValue != metrics.value) {
                metrics.value = nextValue
                metrics.onValueChange(nextValue)
            }
        }
        consumed
    }
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(resolved.toString()) }
    val shown = if (editing) draft else resolved.toString()
    val digitLimit = safeMax.toString().length
    val labelStyle = SteplyTheme.typography.caption.copy(color = colors.text)
    val textMeasurer = rememberTextMeasurer()
    val bandHeight = maxOf(
        maxOf(CenterIndicatorHeight, MajorTickHeight) + MajorLabelGap + LabelRowHeight,
        TouchTarget,
    )

    LaunchedEffect(resolved, min, safeStep, lastIndex, itemExtentPx) {
        val fieldChange = fieldEdit.fromField
        fieldEdit.fromField = false
        if (editing && !fieldChange) draft = resolved.toString()
        val desired = valueIndex(resolved, min, safeStep, lastIndex)
        val current = indexForOffset(offsetState.floatValue, itemExtentPx, lastIndex)
        if (current != desired) offsetState.floatValue = desired * itemExtentPx
    }

    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = VerticalInset),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicTextField(
            value = shown,
            onValueChange = { raw ->
                val digits = raw.filter(Char::isDigit).take(digitLimit)
                draft = digits
                editing = true
                val parsed = digits.toIntOrNull()
                if (parsed != null && parsed in min..safeMax && (parsed - min) % safeStep == 0) {
                    fieldEdit.fromField = true
                    onValueChange(parsed)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(maxOf(ReadoutHeight, TouchTarget))
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        editing = true
                        draft = resolved.toString()
                    } else if (editing) {
                        editing = false
                        val parsed = draft.toIntOrNull()
                        if (parsed != null) {
                            val snapped = snapToStep(parsed, min, safeMax, safeStep)
                            if (snapped != resolved) onValueChange(snapped)
                        }
                    }
                }
                .semantics { this.contentDescription = inputContentDescription },
            textStyle = SteplyTheme.typography.display.copy(
                color = colors.text,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
                fontSize = ValueSize,
                lineHeight = ValueLineHeight,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            cursorBrush = SolidColor(colors.text),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
            },
        )
        Spacer(Modifier.height(SelectedValueGap))
        Box(
            Modifier
                .fillMaxWidth()
                .height(bandHeight)
                .clipToBounds()
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = resolved.toFloat(),
                        range = min.toFloat()..safeMax.toFloat(),
                        steps = ((safeMax - min) / safeStep - 1).coerceAtLeast(0),
                    )
                    setProgress { target ->
                        val snapped = snapToStep(target.roundToInt(), min, safeMax, safeStep)
                        if (snapped != resolved) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onValueChange(snapped)
                        }
                        true
                    }
                }
                .scrollable(
                    state = scrollable,
                    orientation = Orientation.Horizontal,
                    flingBehavior = flingBehavior,
                ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val baseline = maxOf(CenterIndicatorHeight, MajorTickHeight).toPx()
                val labelGap = MajorLabelGap.toPx()
                val labelRow = LabelRowHeight.toPx()
                val minor = MinorTickHeight.toPx()
                val major = MajorTickHeight.toPx()
                val stroke = TickStroke.toPx()
                val offset = offsetState.floatValue
                for (index in 0..lastIndex) {
                    val x = centerX + index * itemExtentPx - offset
                    if (x < 0f || x > size.width) continue
                    val tickHeight = if (index % majorSteps == 0) major else minor
                    drawLine(
                        color = colors.gray,
                        start = Offset(x, baseline - tickHeight),
                        end = Offset(x, baseline),
                        strokeWidth = stroke,
                    )
                    if (index % majorSteps == 0) {
                        val layout = textMeasurer.measure(
                            text = (min + index * safeStep).toString(),
                            style = labelStyle,
                            maxLines = 1,
                            softWrap = false,
                        )
                        drawText(
                            layout,
                            topLeft = Offset(
                                x - layout.size.width / 2f,
                                baseline + labelGap + (labelRow - layout.size.height) / 2f,
                            ),
                        )
                    }
                }
                val indicator = CenterIndicatorWidth.toPx()
                drawLine(
                    color = colors.text,
                    start = Offset(centerX, baseline - CenterIndicatorHeight.toPx()),
                    end = Offset(centerX, baseline),
                    strokeWidth = indicator,
                )
            }
        }
        Spacer(Modifier.height(UnitLabelGap))
        BasicText(
            text = unitLabel,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            style = labelStyle.copy(textAlign = TextAlign.Center),
        )
    }
}

private class FieldEdit {
    var fromField = false
}

private class RulerScrollMetrics {
    var itemExtentPx = 1f
    var maxOffset = 0f
    var lastIndex = 0
    var min = 0
    var step = 1
    var value = 0
    var stopVelocity = 1f
    var onValueChange: (Int) -> Unit = {}
    var performTick: () -> Unit = {}
}

private class RulerFlingBehavior(
    private val offset: () -> Float,
    private val metrics: RulerScrollMetrics,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val limit = metrics.maxOffset
        val extent = metrics.itemExtentPx
        if (extent <= 0f) return 0f
        if (abs(initialVelocity) >= metrics.stopVelocity) {
            glide(offset(), initialVelocity, limit, metrics.stopVelocity, offset)
        }
        animateOffset(offset, snapOffset(offset(), extent, limit))
        return 0f
    }
}

private suspend fun ScrollScope.glide(
    start: Float,
    velocity: Float,
    maxOffset: Float,
    stopVelocity: Float,
    current: () -> Float,
) {
    val dragLog = ln(Friction.toDouble())
    var elapsed = 0.0
    var lastFrame = 0L
    var previous = start
    while (coroutineContext.isActive) {
        val frame = withFrameNanos { it }
        if (lastFrame == 0L) {
            lastFrame = frame
            continue
        }
        elapsed += (frame - lastFrame) / 1_000_000_000.0
        lastFrame = frame
        val decay = Friction.toDouble().pow(elapsed)
        if (abs(velocity * decay) < stopVelocity) break
        val next = (start + velocity * (decay - 1.0) / dragLog).toFloat().coerceIn(0f, maxOffset)
        val delta = next - previous
        if (delta != 0f) scrollBy(delta)
        previous = current()
        if ((previous <= 0f && velocity < 0f) || (previous >= maxOffset && velocity > 0f)) break
    }
}

private suspend fun ScrollScope.animateOffset(current: () -> Float, target: Float) {
    val start = current()
    if (abs(target - start) <= 0.5f) {
        scrollBy(target - start)
        return
    }
    var previous = start
    Animatable(start).animateTo(target, tween(SnapDurationMs, easing = EaseOutCubic)) {
        val delta = value - previous
        if (delta != 0f) scrollBy(delta)
        previous = current()
    }
    val rest = target - current()
    if (abs(rest) > 0.5f) scrollBy(rest)
}

private fun snapOffset(offset: Float, extent: Float, maxOffset: Float): Float {
    if (extent <= 0f) return 0f
    return (indexForOffset(offset, extent, Int.MAX_VALUE) * extent).coerceIn(0f, maxOffset)
}

private fun indexForOffset(offset: Float, extent: Float, lastIndex: Int): Int {
    if (extent <= 0f) return 0
    return (offset / extent).roundToInt().coerceIn(0, lastIndex)
}

private fun valueIndex(value: Int, min: Int, step: Int, lastIndex: Int): Int =
    ((value - min) / step).coerceIn(0, lastIndex)

private fun snapToStep(raw: Int, min: Int, max: Int, step: Int): Int {
    val safeStep = step.coerceAtLeast(1)
    val safeMax = max.coerceAtLeast(min)
    val clamped = raw.coerceIn(min, safeMax)
    val index = ((clamped - min).toFloat() / safeStep).roundToInt()
    return (min + index * safeStep).coerceIn(min, safeMax)
}
