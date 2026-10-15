package io.github.enterenn.steply.data.steps

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

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

/** Moyenne des kcal entières de chaque jour. Défauts de derivedActivity, sans poids ni taille. */
fun averageTrendKcal(bars: List<TrendDayBar>): Int {
    if (bars.isEmpty()) return 0
    val size = bars.size.toLong()
    val sum = bars.sumOf { derivedActivity(it.steps).kilocalories.toLong() }
    return ((sum + size / 2) / size).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}

/** Plus haut total strictement positif. À égalité, la barre la plus récente. */
fun peakTrendDay(bars: List<TrendDayBar>): TrendDayBar? =
    bars.withIndex()
        .filter { (_, bar) -> bar.steps > 0L }
        .maxWithOrNull(compareBy({ it.value.steps }, { it.index }))
        ?.value

/** Plus bas total strictement positif. À égalité, la barre la plus récente. */
fun lowTrendDay(bars: List<TrendDayBar>): TrendDayBar? =
    bars.withIndex()
        .filter { (_, bar) -> bar.steps > 0L }
        .minWithOrNull(compareBy({ it.value.steps }, { -it.index }))
        ?.value

/**
 * L'axe dépasse le plus haut objectif et le jour le plus haut, pour que la ligne
 * et la barre ne collent pas au bord. Si un jour dépasse cet objectif, le sommet
 * est arrondi au millier supérieur afin que le libellé tienne dans l'axe.
 */
fun trendChartMax(peakSteps: Long, goals: List<Int>): Int {
    val peak = peakSteps.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    val goal = goals.maxOrNull()?.coerceAtLeast(0) ?: 0
    val base = maxOf(peak, goal)
    val padded = ceilDiv(base.toLong() * (100 + ChartHeadroomPercent), 100L)
    if (peak <= goal) return padded.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val rounded = ceilDiv(padded, 1_000L) * 1_000L
    return rounded.coerceIn(padded, Int.MAX_VALUE.toLong()).toInt()
}

fun trendAxisTickValues(maxValue: Int, goals: List<Int>): List<Int> {
    if (maxValue <= 0) return listOf(0)
    val ticks = mutableListOf(0)
    ticks += goals.filter { it in 1 until maxValue }.distinct().sorted()
    val highestGoal = goals.maxOrNull() ?: 0
    val gap = (maxValue - highestGoal).toFloat() / maxValue.toFloat()
    if (maxValue > highestGoal && gap >= AxisTickMinGap) ticks += maxValue
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
