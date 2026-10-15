package io.github.enterenn.steply.data.steps

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

const val TrendGoalSteps = 10_000

data class TrendDayBar(
    val day: LocalDate,
    val steps: Long,
)

fun trendDayBars(
    readings: List<StepReading>,
    today: LocalDate,
    dayCount: Int,
    zone: ZoneId,
): List<TrendDayBar> {
    if (dayCount <= 0) return emptyList()
    val start = today.minusDays((dayCount - 1).toLong())
    return List(dayCount) { index ->
        val day = start.plusDays(index.toLong())
        TrendDayBar(day, daySteps(readings, day, zone))
    }
}

fun averageTrendSteps(bars: List<TrendDayBar>): Int {
    if (bars.isEmpty()) return 0
    val size = bars.size.toLong()
    val sum = bars.sumOf { it.steps }
    return ((sum + size / 2) / size).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}

/**
 * L'axe dépasse l'objectif et le jour le plus haut, pour que la ligne
 * et la barre ne collent pas au bord. Si un jour dépasse 10 000, le sommet
 * est arrondi au millier supérieur afin que le libellé tienne dans l'axe.
 */
fun trendChartMax(peakSteps: Long, goal: Int = TrendGoalSteps): Int {
    val peak = peakSteps.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    val base = maxOf(peak, goal)
    val padded = ceilDiv(base.toLong() * (100 + ChartHeadroomPercent), 100L)
    if (peak <= goal) return padded.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val rounded = ceilDiv(padded, 1_000L) * 1_000L
    return rounded.coerceIn(padded, Int.MAX_VALUE.toLong()).toInt()
}

fun trendAxisTickValues(maxValue: Int, goal: Int = TrendGoalSteps): List<Int> {
    if (maxValue <= 0) return listOf(0)
    val ticks = mutableListOf(0)
    if (goal in 1 until maxValue) ticks += goal
    val gap = (maxValue - goal).toFloat() / maxValue.toFloat()
    if (maxValue > goal && gap >= AxisTickMinGap) ticks += maxValue
    return ticks
}

fun formatTrendAxisTick(value: Int): String =
    if (value >= 1_000 && value % 1_000 == 0) "${value / 1_000}k" else value.toString()

fun groupThousands(steps: Long): String {
    val digits = steps.coerceAtLeast(0L).toString()
    return digits.reversed().chunked(3).joinToString(" ").reversed()
}

/** Sur 30 barres, un libellé tous les cinq jours, plus aujourd'hui. */
fun showsTrendBarLabel(index: Int, barCount: Int): Boolean {
    if (barCount <= 7) return index in 0 until barCount
    val step = ceil(barCount / 6.0).toInt().coerceAtLeast(1)
    return index % step == 0 || index == barCount - 1
}

private const val ChartHeadroomPercent = 5
private const val AxisTickMinGap = 0.15f

private fun ceilDiv(numerator: Long, denominator: Long): Long =
    (numerator + denominator - 1) / denominator
