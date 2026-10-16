package io.github.enterenn.steply.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

const val TrendMonthCount = 12

data class TrendMonthBar(
    val lastDay: LocalDate,
    val steps: Long,
)

fun trendYearDayCount(today: LocalDate): Int =
    (ChronoUnit.DAYS.between(trendYearStart(today), today) + 1).toInt()

/** Somme des daySteps de chaque mois. Le mois courant s'arrête à today. */
fun trendMonthBars(
    readings: List<StepReading>,
    today: LocalDate,
    zone: ZoneId,
): List<TrendMonthBar> {
    val first = YearMonth.from(trendYearStart(today))
    val current = YearMonth.from(today)
    return List(TrendMonthCount) { index ->
        val month = first.plusMonths(index.toLong())
        val last = if (month == current) today else month.atEndOfMonth()
        var steps = 0L
        var day = month.atDay(1)
        while (!day.isAfter(last)) {
            steps += daySteps(readings, day, zone)
            day = day.plusDays(1)
        }
        TrendMonthBar(lastDay = last, steps = steps)
    }
}

private fun trendYearStart(today: LocalDate): LocalDate =
    YearMonth.from(today).minusMonths((TrendMonthCount - 1).toLong()).atDay(1)
