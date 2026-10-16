package io.github.enterenn.steply.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.enterenn.steply.R
import io.github.enterenn.steply.ui.theme.SteplyTheme
import kotlin.math.min
import kotlin.math.roundToInt

internal val RingStroke = 24.dp
private val RingMinDiameter = 240.dp
private val RingMaxDiameter = 300.dp
private const val RingWidthFactor = 0.80f
internal const val RingStartAngle = -90f
internal const val RingFullSweep = 360f
private val RingIconSize = 20.sp
private val RingLabelSize = 16.sp
private val RingLabelLineHeight = 18.sp
private val RingValueSize = 64.sp
private val RingValueLineHeight = 64.sp
private val RingGoalSize = 16.sp
private val RingGoalLineHeight = 18.sp
private const val StepsGlyph = "\uED60"

private val ChartPlotHeight = 220.dp
private val ChartAxisWidth = 36.dp
private val ChartBottomAxis = 24.dp
private val BarMinWidth = 4.dp
private val BarMaxWidth = 12.dp
private const val BarSlotFill = 0.55f
private val BarTopCorner = 4.dp
private val GoalLineStroke = 1.5.dp
private val GoalDash = 6.dp
private val GoalGap = 4.dp

private val PhosphorFill = FontFamily(Font(R.font.phosphor_fill))

data class SteplyChartTick(val value: Int, val label: String)

@Composable
fun SteplyProgressRing(
    label: String,
    valueText: String,
    goalText: String,
    value: Int,
    goal: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    celebrating: Boolean = false,
    onCelebrationFinished: () -> Unit = {},
) {
    val colors = SteplyTheme.colors
    val spacing = SteplyTheme.spacing
    val spokenLabel = contentDescription
    val stepMotion = rememberStepMotion(value, valueText, goal)
    val celebration = rememberGoalCelebration(celebrating, onCelebrationFinished)
    val celebrationStart = remember(celebrating) {
        if (!celebrating || goal <= 0) 0f else (value.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    }
    val arcFraction = if (celebrating) celebration.arcFraction(celebrationStart) else null
    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val diameter = (maxWidth * RingWidthFactor)
            .coerceIn(RingMinDiameter, RingMaxDiameter)
            .coerceAtMost(maxWidth)
        Box(
            Modifier
                .size(diameter)
                .semantics(mergeDescendants = true) {
                    this.contentDescription = spokenLabel
                    if (goal > 0) {
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = value.toFloat().coerceIn(0f, goal.toFloat()),
                            range = 0f..goal.toFloat(),
                        )
                    }
                },
        ) {
            if (celebrating) {
                GoalRingCelebrationLayers(
                    playback = celebration,
                    accent = colors.accent,
                    muted = lerp(colors.accent, colors.lightGray, 0.45f),
                    sparkle = colors.text,
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        if (!celebrating) return@graphicsLayer
                        val scale = celebration.scale()
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
            RingArc(stepMotion, colors.lightGray, colors.accent, arcFraction)
            if (celebrating) GoalRingShimmer(celebration.shimmer(), colors.accent)
            ScaleDown(
                Modifier
                    .fillMaxSize()
                    .padding(RingStroke)
                    .clearAndSetSemantics {},
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            text = StepsGlyph,
                            style = TextStyle(
                                fontFamily = PhosphorFill,
                                fontSize = RingIconSize,
                                color = colors.text,
                            ),
                        )
                        BasicText(
                            text = label,
                            modifier = Modifier.padding(start = spacing.inline / 2),
                            style = SteplyTheme.typography.body.copy(
                                color = colors.text,
                                fontSize = RingLabelSize,
                                lineHeight = RingLabelLineHeight,
                                fontWeight = FontWeight.W500,
                            ),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    val valueStyle = SteplyTheme.typography.display.copy(
                        color = colors.text,
                        fontSize = RingValueSize,
                        lineHeight = RingValueLineHeight,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    StepCountLabel(
                        motion = stepMotion,
                        current = valueText,
                        style = valueStyle,
                    )
                    BasicText(
                        text = goalText,
                        style = SteplyTheme.typography.display.copy(
                            color = colors.text,
                            fontSize = RingGoalSize,
                            lineHeight = RingGoalLineHeight,
                            fontWeight = FontWeight.W500,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun rememberStepMotion(value: Int, text: String, goal: Int): StepMotion {
    val motionEnabled = motionEnabled()
    val state = remember {
        StepMotion(targetFraction(value, goal)).also {
            it.settledValue = value
            it.settledText = text
        }
    }
    LaunchedEffect(value, text, goal, motionEnabled) {
        val target = targetFraction(value, goal)
        val delta = value - state.settledValue
        val tick = motionEnabled && delta in 1..MicroTickMaxDelta && text != state.settledText
        if (!tick) {
            state.ticking = false
            state.fraction = target
            state.settledValue = value
            state.settledText = text
            return@LaunchedEffect
        }
        state.fromText = state.settledText
        state.fromFraction = state.displayedFraction()
        state.targetFraction = target
        state.settledValue = value
        state.settledText = text
        state.ticking = true
        state.tick = 0f
        Animatable(0f).animateTo(1f, tween(MicroTickMs, easing = MicroTickEasing)) {
            state.tick = this.value
        }
        state.fraction = target
        state.ticking = false
        state.tick = 1f
    }
    return state
}

@Composable
private fun BoxScope.RingArc(
    motion: StepMotion,
    trackColor: Color,
    arcColor: Color,
    fractionOverride: Float? = null,
) {
    val fraction = fractionOverride ?: motion.displayedFraction()
    Canvas(Modifier.matchParentSize()) {
        val stroke = RingStroke.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension - stroke) / 2f
        if (radius <= 0f) return@Canvas
        val outer = radius + stroke / 2f
        val inner = radius - stroke / 2f
        val track = Path().apply {
            fillType = PathFillType.EvenOdd
            addOval(Rect(center, outer))
            addOval(Rect(center, inner))
        }
        drawPath(track, trackColor)
        if (fraction > 0f) {
            drawArc(
                color = arcColor,
                startAngle = RingStartAngle,
                sweepAngle = RingFullSweep * fraction,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun StepCountLabel(motion: StepMotion, current: String, style: TextStyle) {
    if (motion.ticking) {
        TickStepCount(current = current, previous = motion.fromText, motion = motion, style = style)
    } else {
        BasicText(text = motion.settledText, style = style, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun TickStepCount(
    current: String,
    previous: String,
    motion: StepMotion,
    style: TextStyle,
) {
    val count = maxOf(current.length, previous.length)
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            TickDigit(
                current = current.segmentAt(index),
                previous = previous.segmentAt(index),
                motion = motion,
                style = style,
            )
        }
    }
}

@Composable
private fun TickDigit(
    current: String,
    previous: String,
    motion: StepMotion,
    style: TextStyle,
) {
    if (current == previous || current.isEmpty()) {
        BasicText(text = current, style = style, maxLines = 1, softWrap = false)
        return
    }
    val eased = motion.tick.coerceIn(0f, 1f)
    val slide = MicroTickSlide * (1f - eased)
    val density = LocalDensity.current
    val cellHeight = with(density) { style.lineHeight.toDp() }
    Box(
        Modifier
            .height(cellHeight)
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = previous,
            modifier = Modifier.graphicsLayer {
                translationY = -slide.toPx()
                alpha = 1f - eased
            },
            style = style,
            maxLines = 1,
            softWrap = false,
        )
        BasicText(
            text = current,
            modifier = Modifier.graphicsLayer {
                translationY = slide.toPx()
                alpha = eased
            },
            style = style,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun motionEnabled(): Boolean {
    val context = LocalContext.current
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale > 0f
}

private fun targetFraction(value: Int, goal: Int): Float {
    if (goal <= 0) return 0f
    return (value.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
}

private fun String.segmentAt(index: Int): String =
    if (index < length) this[index].toString() else ""

private class StepMotion(fraction: Float) {
    var fraction by mutableFloatStateOf(fraction)
    var fromFraction by mutableFloatStateOf(fraction)
    var targetFraction by mutableFloatStateOf(fraction)
    var settledValue by mutableIntStateOf(0)
    var settledText by mutableStateOf("")
    var fromText by mutableStateOf("")
    var tick by mutableFloatStateOf(1f)
    var ticking by mutableStateOf(false)

    fun displayedFraction(): Float {
        return if (ticking) fromFraction + (targetFraction - fromFraction) * tick else fraction
    }
}

private const val MicroTickMaxDelta = 15
private const val MicroTickMs = 150
private val MicroTickSlide = 5.dp
private val MicroTickEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

@Composable
fun SteplyHistogram(
    values: List<Int>,
    maxValue: Int,
    goalValues: List<Int>,
    ticks: List<SteplyChartTick>,
    dayLabels: List<String>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = SteplyTheme.colors
    val spokenLabel = contentDescription
    val figureStyle = SteplyTheme.typography.display.copy(
        color = colors.text,
        fontSize = SteplyTheme.typography.caption.fontSize,
        lineHeight = SteplyTheme.typography.caption.lineHeight,
        fontWeight = FontWeight.W500,
        textAlign = TextAlign.End,
    )
    val dayStyle = SteplyTheme.typography.caption.copy(
        color = colors.text,
        textAlign = TextAlign.Center,
    )
    Column(
        modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Image
                this.contentDescription = spokenLabel
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(ChartPlotHeight),
        ) {
            ChartYAxis(
                ticks = ticks,
                maxValue = maxValue,
                style = figureStyle,
            )
            ChartPlot(
                values = values,
                maxValue = maxValue,
                goalValues = goalValues,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(ChartBottomAxis),
        ) {
            Box(Modifier.width(ChartAxisWidth))
            ChartDayLabels(
                labels = dayLabels,
                barCount = values.size,
                style = dayStyle,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ScaleDown(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeable = measurables.first().measure(Constraints())
        val maxWidth = constraints.maxWidth
        val maxHeight = constraints.maxHeight
        if (maxWidth == Constraints.Infinity || maxHeight == Constraints.Infinity) {
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        } else {
            val scale = min(
                1f,
                min(
                    maxWidth / placeable.width.coerceAtLeast(1).toFloat(),
                    maxHeight / placeable.height.coerceAtLeast(1).toFloat(),
                ),
            )
            val scaledWidth = placeable.width * scale
            val scaledHeight = placeable.height * scale
            layout(maxWidth, maxHeight) {
                placeable.placeRelativeWithLayer(
                    x = ((maxWidth - scaledWidth) / 2f).roundToInt(),
                    y = ((maxHeight - scaledHeight) / 2f).roundToInt(),
                ) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }
}

@Composable
private fun ChartYAxis(
    ticks: List<SteplyChartTick>,
    maxValue: Int,
    style: TextStyle,
) {
    BoxWithConstraints(
        Modifier
            .width(ChartAxisWidth)
            .fillMaxHeight()
            .padding(end = SteplyTheme.spacing.inline / 2),
    ) {
        ticks.forEach { tick ->
            val fraction = tickFraction(tick.value, maxValue)
            BasicText(
                text = tick.label,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = maxHeight * (fraction - 0.5f))
                    .clearAndSetSemantics {},
                style = style,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun ChartPlot(
    values: List<Int>,
    maxValue: Int,
    goalValues: List<Int>,
    modifier: Modifier = Modifier,
) {
    val barColor = SteplyTheme.colors.accent
    Canvas(modifier.fillMaxSize().clearAndSetSemantics {}) {
        if (values.isEmpty() || maxValue <= 0 || size.width <= 0f || size.height <= 0f) return@Canvas
        val barWidth = barWidthPx(size.width, values.size, BarMinWidth.toPx(), BarMaxWidth.toPx())
        val centers = barCentersPx(size.width, values.size, barWidth)
        val cornerLimit = BarTopCorner.toPx().coerceAtMost(barWidth / 2f)
        values.forEachIndexed { index, value ->
            if (value <= 0 || index >= centers.size) return@forEachIndexed
            val barHeight = size.height * (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
            val left = centers[index] - barWidth / 2f
            val top = size.height - barHeight
            val corner = cornerLimit.coerceAtMost(barHeight / 2f)
            drawPath(
                Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = top,
                            right = left + barWidth,
                            bottom = size.height,
                            topLeftCornerRadius = CornerRadius(corner),
                            topRightCornerRadius = CornerRadius(corner),
                        ),
                    )
                },
                barColor,
            )
        }
        val count = minOf(values.size, goalValues.size)
        if (count > 0) {
            val slot = size.width / values.size.toFloat()
            var start = 0
            while (start < count) {
                val goal = goalValues[start]
                var end = start + 1
                while (end < count && goalValues[end] == goal) end++
                if (goal > 0 && goal <= maxValue) {
                    val y = size.height * (1f - goal.toFloat() / maxValue.toFloat())
                    drawLine(
                        color = barColor,
                        start = Offset(start * slot, y),
                        end = Offset(end * slot, y),
                        strokeWidth = GoalLineStroke.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(GoalDash.toPx(), GoalGap.toPx()),
                        ),
                    )
                }
                start = end
            }
        }
    }
}

@Composable
private fun ChartDayLabels(
    labels: List<String>,
    barCount: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        if (barCount <= 0) return@BoxWithConstraints
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val barWidth = barWidthPx(
            widthPx,
            barCount,
            with(density) { BarMinWidth.toPx() },
            with(density) { BarMaxWidth.toPx() },
        )
        val centers = barCentersPx(widthPx, barCount, barWidth)
        labels.forEachIndexed { index, label ->
            if (index >= centers.size || widthPx <= 0f) return@forEachIndexed
            val fraction = centers[index] / widthPx
            BasicText(
                text = label,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = maxWidth * (fraction - 0.5f))
                    .clearAndSetSemantics {},
                style = style,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

private fun tickFraction(value: Int, maxValue: Int): Float {
    if (maxValue <= 0) return 1f
    return (1f - value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
}

private fun barWidthPx(chartWidthPx: Float, pointCount: Int, minPx: Float, maxPx: Float): Float {
    if (pointCount <= 0 || chartWidthPx <= 0f) return maxPx
    return (chartWidthPx / pointCount * BarSlotFill).coerceIn(minPx, maxPx)
}

private fun barCentersPx(viewWidthPx: Float, barCount: Int, barWidthPx: Float): FloatArray {
    if (barCount <= 0 || viewWidthPx <= 0f) return floatArrayOf()
    val eachSpace = (viewWidthPx - barWidthPx * barCount) / (barCount * 2f)
    val centers = FloatArray(barCount)
    var cursor = 0f
    for (index in 0 until barCount) {
        cursor += eachSpace + barWidthPx / 2f
        centers[index] = cursor
        cursor += barWidthPx / 2f + eachSpace
    }
    return centers
}
