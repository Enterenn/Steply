package io.github.enterenn.steply.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val SequenceMs = 4_000f
private const val ReducedMotionMs = 500
private const val ArcSweepMs = 400f
private const val PulseMs = 720f
private const val PulsePeak = 1.08f
private const val HapticDelayMs = 260L
private const val ParticleCount = 45
private const val TwinkleStartAge = 0.58f

private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

internal class GoalCelebrationPlayback {
    var t by mutableFloatStateOf(0f)
    var reduceMotion by mutableStateOf(false)

    fun scale(): Float {
        if (reduceMotion) return 1f
        val end = PulseMs / SequenceMs
        if (t <= 0f || t >= end) return 1f
        val bump = sin((t / end) * PI).toFloat()
        return 1f + (PulsePeak - 1f) * bump
    }

    fun glow(): Float = if (reduceMotion) 0f else pulseOpacity(t, 1_200f, 0.30f)

    fun shimmer(): Float = if (reduceMotion) 0f else windowPulse(t, 550f, 1_100f, 0.22f)

    fun arcFraction(start: Float): Float {
        if (reduceMotion) return 1f
        val sweepT = (t * SequenceMs / ArcSweepMs).coerceIn(0f, 1f)
        val eased = EaseInOut.transform(sweepT)
        return start + (1f - start) * eased
    }
}

@Composable
internal fun rememberGoalCelebration(
    enabled: Boolean,
    onFinished: () -> Unit,
): GoalCelebrationPlayback {
    val playback = remember { GoalCelebrationPlayback() }
    val reduceMotion = !motionEnabled()
    val view = LocalView.current
    if (enabled) playback.reduceMotion = reduceMotion
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        playback.reduceMotion = reduceMotion
        playback.t = 0f
        var finished = false
        try {
            if (!reduceMotion) {
                launch {
                    delay(HapticDelayMs)
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
            val duration = if (reduceMotion) ReducedMotionMs else SequenceMs.toInt()
            Animatable(0f).animateTo(1f, tween(duration, easing = LinearEasing)) {
                playback.t = value
            }
            finished = true
            onFinished()
        } finally {
            if (!finished && !isActive) onFinished()
        }
    }
    return playback
}

@Composable
internal fun BoxScope.GoalRingCelebrationLayers(
    playback: GoalCelebrationPlayback,
    accent: Color,
    muted: Color,
    sparkle: Color,
) {
    if (playback.reduceMotion) return
    val particles = remember { celebrationParticles() }
    val t = playback.t
    val glow = playback.glow()
    BoxWithConstraints(Modifier.matchParentSize()) {
        val side = maxWidth * 1.45f
        Canvas(Modifier.size(side).align(Alignment.Center)) {
            val stroke = RingStroke.toPx()
            val ringSide = size.minDimension / 1.45f
            val ringRadius = (ringSide - stroke) / 2f
            if (ringRadius <= 0f) return@Canvas
            if (glow > 0f) {
                val radius = ringRadius + stroke * 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = glow), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
            paintCelebrationParticles(
                t = t,
                ringRadius = ringRadius,
                accent = accent,
                muted = muted,
                sparkle = sparkle,
                particles = particles,
            )
        }
    }
}

@Composable
internal fun BoxScope.GoalRingShimmer(strength: Float, color: Color) {
    if (strength <= 0f) return
    Canvas(Modifier.matchParentSize()) {
        val stroke = RingStroke.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension - stroke) / 2f
        if (radius <= 0f) return@Canvas
        drawArc(
            color = color.copy(alpha = strength.coerceIn(0f, 1f)),
            startAngle = RingStartAngle,
            sweepAngle = RingFullSweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

private fun pulseOpacity(t: Float, durationMs: Float, peak: Float): Float {
    val end = durationMs / SequenceMs
    if (t <= 0f || t >= end) return 0f
    val local = t / end
    val shaped = if (local <= 0.5f) {
        EaseOut.transform(local * 2f)
    } else {
        EaseOut.transform((1f - local) * 2f)
    }
    return peak * shaped
}

private fun windowPulse(t: Float, startMs: Float, endMs: Float, peak: Float): Float {
    val start = startMs / SequenceMs
    val end = endMs / SequenceMs
    if (t < start || t > end) return 0f
    val local = (t - start) / (end - start)
    val shaped = if (local <= 0.5f) local * 2f else (1f - local) * 2f
    return peak * EaseInOut.transform(shaped)
}

private data class CelebrationParticle(
    val angle: Float,
    val normalizedSpeed: Float,
    val size: Float,
    val startOffset: Float,
    val lifespan: Float,
    val useMutedColor: Boolean,
    val useSparkle: Boolean,
)

private fun celebrationParticles(): List<CelebrationParticle> {
    val random = Random(42)
    return List(ParticleCount) {
        CelebrationParticle(
            angle = (random.nextDouble() * PI * 2).toFloat(),
            normalizedSpeed = (0.4 + random.nextDouble() * 0.6).toFloat(),
            size = (1 + random.nextDouble() * 4).toFloat(),
            startOffset = (0.78 + random.nextDouble() * 0.22).toFloat(),
            lifespan = (0.20 + random.nextDouble() * 0.26).toFloat(),
            useMutedColor = random.nextDouble() > 0.45,
            useSparkle = random.nextDouble() > 0.82,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.paintCelebrationParticles(
    t: Float,
    ringRadius: Float,
    accent: Color,
    muted: Color,
    sparkle: Color,
    particles: List<CelebrationParticle>,
) {
    val envelope = burstEnvelope(t)
    if (envelope <= 0f) return
    for (particle in particles) {
        val birth = particle.startOffset * 0.018f
        val age = (t - birth) / particle.lifespan
        if (age < 0f || age > 1f) continue
        val easedAge = easeOutQuart(age)
        val drift = ringRadius * particle.normalizedSpeed * 0.72f * easedAge
        val distance = ringRadius * particle.startOffset + drift
        val gravity = easedAge * easedAge * ringRadius * 0.08f
        val offset = Offset(
            center.x + cos(particle.angle) * distance,
            center.y + sin(particle.angle) * distance + gravity,
        )
        var fade = (1f - age) * envelope
        if (fade <= 0.02f) continue
        var base = when {
            particle.useSparkle -> sparkle
            particle.useMutedColor -> muted
            else -> accent
        }
        var radius = particle.size / 2f
        var alpha = fade * 0.9f
        if (age >= TwinkleStartAge) {
            val tail = (age - TwinkleStartAge) / (1f - TwinkleStartAge)
            val twinkle = exitTwinkle(age, particle.angle)
            val intensity = if (particle.useSparkle) 1f else 0.72f
            alpha *= 0.62f + (0.38f + 0.42f * intensity) * twinkle
            base = lerp(
                base,
                sparkle,
                (tail * twinkle * (0.45f + 0.35f * intensity)).coerceIn(0f, 1f),
            )
            radius *= 1f + (0.18f + 0.12f * intensity) * tail * twinkle
        }
        if (alpha <= 0.02f) continue
        drawCircle(
            color = base.copy(alpha = alpha.coerceIn(0f, 1f)),
            radius = if (particle.useSparkle) radius * 0.7f else radius,
            center = offset,
        )
    }
}

private fun burstEnvelope(t: Float): Float = when {
    t < 0.006f -> t / 0.006f
    t < 0.38f -> 1f
    t < 0.75f -> (0.75f - t) / 0.37f
    else -> 0f
}

private fun easeOutQuart(t: Float): Float {
    val remaining = 1f - t.coerceIn(0f, 1f)
    return 1f - remaining * remaining * remaining * remaining
}

private fun exitTwinkle(age: Float, phase: Float): Float =
    0.5f + 0.5f * sin(phase * 6.1f + age * 38f).toFloat()
