package io.github.enterenn.steply.ui.trends

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.enterenn.steply.R
import io.github.enterenn.steply.core.di.AppContainer
import io.github.enterenn.steply.domain.DailyGoalDefault
import io.github.enterenn.steply.domain.DailyGoalHistory
import io.github.enterenn.steply.domain.Profile
import io.github.enterenn.steply.domain.TrendDayBar
import io.github.enterenn.steply.domain.TrendMonthBar
import io.github.enterenn.steply.domain.TrendMonthCount
import io.github.enterenn.steply.domain.goalsForDays
import io.github.enterenn.steply.domain.averageTrendKcal
import io.github.enterenn.steply.domain.averageTrendSteps
import io.github.enterenn.steply.domain.formatTrendAxisTick
import io.github.enterenn.steply.domain.groupThousands
import io.github.enterenn.steply.domain.lowTrendDay
import io.github.enterenn.steply.domain.peakTrendDay
import io.github.enterenn.steply.domain.showsTrendBarLabel
import io.github.enterenn.steply.domain.trendAxisTickValues
import io.github.enterenn.steply.domain.trendChartMax
import io.github.enterenn.steply.domain.trendDayBars
import io.github.enterenn.steply.domain.trendMonthBars
import io.github.enterenn.steply.domain.trendYearDayCount
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.components.SteplyChartTick
import io.github.enterenn.steply.ui.components.SteplyListRow
import io.github.enterenn.steply.ui.components.SteplyHistogram
import io.github.enterenn.steply.ui.components.SteplySegmentedControl
import io.github.enterenn.steply.ui.components.SteplyStatTile
import io.github.enterenn.steply.ui.theme.SteplyTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TrendsScreen(container: AppContainer) {
    val spacing = SteplyTheme.spacing
    var period by remember { mutableIntStateOf(PeriodSevenDays) }
    val month = period == PeriodThirtyDays
    val year = period == PeriodTwelveMonths
    val today = LocalDate.now()
    val dayCount = if (year) {
        trendYearDayCount(today)
    } else if (month) {
        ThirtyDayCount
    } else {
        SevenDayCount
    }
    val readings by container.stepReadings.collectAsState()
    val goal by container.dailyGoal.collectAsState(initial = DailyGoalDefault)
    val goalHistory by container.dailyGoalHistory.collectAsState(initial = DailyGoalHistory())
    val profile by container.profile.collectAsState(initial = Profile())
    var bars by remember { mutableStateOf<List<TrendDayBar>>(emptyList()) }
    var monthBars by remember { mutableStateOf<List<TrendMonthBar>>(emptyList()) }
    LaunchedEffect(dayCount, readings, year, today) {
        val zone = ZoneId.systemDefault()
        if (year) {
            bars = container.stepCounterRepository.trendBars(dayCount, today, zone)
            monthBars = container.stepCounterRepository.trendMonthBars(today, zone)
        } else {
            bars = container.stepCounterRepository.trendBars(dayCount)
            monthBars = emptyList()
        }
    }
    val chartBars = if (bars.size == dayCount) {
        bars
    } else {
        trendDayBars(emptyList(), today, dayCount, ZoneId.systemDefault())
    }
    val plottedMonths = when {
        !year -> emptyList()
        monthBars.size == TrendMonthCount && monthBars.last().lastDay == today -> monthBars
        else -> trendMonthBars(emptyList(), today, ZoneId.systemDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.stack),
    ) {
        BasicText(
            text = stringResource(R.string.tab_trends),
            modifier = Modifier.semantics { heading() },
            style = SteplyTheme.typography.body.copy(color = SteplyTheme.colors.gray),
        )
        SteplySegmentedControl(
            options = listOf(
                stringResource(R.string.trends_period_7),
                stringResource(R.string.trends_period_30),
                stringResource(R.string.trends_period_12),
            ),
            selectedIndex = period,
            onSelect = { period = it },
        )
        if (year) {
            BasicText(
                text = yearRange(plottedMonths),
                modifier = Modifier
                    .clip(RoundedCornerShape(SteplyTheme.radius.control))
                    .background(SteplyTheme.colors.lightGray)
                    .padding(horizontal = spacing.card, vertical = spacing.inline / 2),
                style = SteplyTheme.typography.caption.copy(color = SteplyTheme.colors.text),
            )
        }
        SteplyCard {
            if (year) {
                val monthGoals = goalsOnDays(plottedMonths.map { it.lastDay }, goal, goalHistory)
                val maxValue = trendChartMax(
                    plottedMonths.maxOfOrNull { it.steps } ?: 0L,
                    monthGoals,
                )
                SteplyHistogram(
                    values = plottedMonths.map { bar ->
                        bar.steps.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                    },
                    maxValue = maxValue,
                    goalValues = monthGoals,
                    ticks = trendAxisTickValues(maxValue, monthGoals).map { value ->
                        SteplyChartTick(value, formatTrendAxisTick(value))
                    },
                    dayLabels = monthBarLabels(plottedMonths),
                    contentDescription = yearChartDescription(plottedMonths),
                )
            } else {
                val dayGoals = periodGoals(chartBars, goal, goalHistory)
                val maxValue = trendChartMax(
                    chartBars.maxOfOrNull { it.steps } ?: 0L,
                    dayGoals,
                )
                SteplyHistogram(
                    values = chartBars.map { bar ->
                        bar.steps.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                    },
                    maxValue = maxValue,
                    goalValues = dayGoals,
                    ticks = trendAxisTickValues(maxValue, dayGoals).map { value ->
                        SteplyChartTick(value, formatTrendAxisTick(value))
                    },
                    dayLabels = chartDayLabels(chartBars),
                    contentDescription = chartDescription(chartBars, dayGoals),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.stack),
        ) {
            SteplyStatTile(
                glyph = FireGlyph,
                value = run {
                    val weightKg = profile.weightKg?.toDouble()
                    val kcal = if (weightKg == null) {
                        averageTrendKcal(chartBars)
                    } else {
                        averageTrendKcal(chartBars, weightKg = weightKg)
                    }
                    groupThousands(kcal.toLong())
                },
                unit = stringResource(R.string.gallery_stat_kcal_unit),
                caption = stringResource(R.string.gallery_stat_kcal_caption),
                modifier = Modifier.weight(1f),
            )
            SteplyStatTile(
                glyph = FootprintsGlyph,
                value = groupThousands(averageTrendSteps(chartBars).toLong()),
                unit = stringResource(R.string.gallery_stat_steps_unit),
                caption = stringResource(R.string.gallery_stat_steps_caption),
                modifier = Modifier.weight(1f),
            )
        }
        if (peakTrendDay(chartBars) != null) {
            PeriodSummaryCard(
                bars = chartBars,
                sevenDays = period == PeriodSevenDays,
                withYear = year,
            )
        }
    }
}

@Composable
private fun PeriodSummaryCard(bars: List<TrendDayBar>, sevenDays: Boolean, withYear: Boolean) {
    val peak = peakTrendDay(bars) ?: return
    val low = lowTrendDay(bars)?.takeUnless { it.day == peak.day }
    val spacing = SteplyTheme.spacing
    val unit = stringResource(R.string.gallery_stat_steps_unit)
    SteplyCard(contentPadding = PaddingValues(0.dp)) {
        BasicText(
            text = stringResource(R.string.trends_summary_title),
            modifier = Modifier
                .padding(
                    start = spacing.card,
                    top = spacing.card,
                    end = spacing.card,
                    bottom = spacing.inline,
                )
                .semantics { heading() },
            style = SteplyTheme.typography.title.copy(color = SteplyTheme.colors.text),
        )
        SteplyListRow(
            label = stringResource(R.string.trends_peak_label),
            value = dayRowValue(peak, sevenDays, withYear, unit),
        )
        if (low != null) {
            SteplyListRow(
                label = stringResource(R.string.trends_low_label),
                value = dayRowValue(low, sevenDays, withYear, unit),
            )
        }
        SteplyListRow(
            label = stringResource(R.string.trends_total_label),
            value = "${groupThousands(bars.sumOf { it.steps })} $unit",
        )
    }
}

@Composable
private fun dayRowValue(
    bar: TrendDayBar,
    sevenDays: Boolean,
    withYear: Boolean,
    unit: String,
): String {
    val date = when {
        sevenDays -> "${stringResource(chartAbbreviation(bar.day.dayOfWeek))} ${bar.day.dayOfMonth}"
        withYear -> "${bar.day.dayOfMonth}/${bar.day.monthValue}/${bar.day.year}"
        else -> "${bar.day.dayOfMonth}/${bar.day.monthValue}"
    }
    return "$date, ${groupThousands(bar.steps)} $unit"
}

@Composable
private fun chartDayLabels(bars: List<TrendDayBar>): List<String> {
    val labels = ArrayList<String>(bars.size)
    for ((index, bar) in bars.withIndex()) {
        labels += when {
            !showsTrendBarLabel(index, bars.size) -> ""
            bars.size <= SevenDayCount -> stringResource(chartAbbreviation(bar.day.dayOfWeek))
            else -> "${bar.day.dayOfMonth}/${bar.day.monthValue}"
        }
    }
    return labels
}

private fun periodGoals(
    bars: List<TrendDayBar>,
    currentGoal: Int,
    history: DailyGoalHistory,
): List<Int> = goalsOnDays(bars.map { it.day }, currentGoal, history)

private fun goalsOnDays(
    days: List<LocalDate>,
    currentGoal: Int,
    history: DailyGoalHistory,
): List<Int> {
    val recordedStart = history.startingGoal
    return if (recordedStart == null) {
        goalsForDays(days, currentGoal, emptyList())
    } else {
        goalsForDays(days, recordedStart, history.changes)
    }
}

@Composable
private fun yearRange(months: List<TrendMonthBar>): String {
    val first = months.firstOrNull() ?: return ""
    val format = DateTimeFormatter.ofPattern("MMM yyyy", chartLocale())
    return stringResource(
        R.string.trends_range_12,
        format.format(first.lastDay),
        format.format(months.last().lastDay),
    )
}

@Composable
private fun yearChartDescription(months: List<TrendMonthBar>): String {
    val first = months.firstOrNull() ?: return ""
    val locale = chartLocale()
    val full = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    val monthOnly = DateTimeFormatter.ofPattern("MMMM", locale)
    val parts = ArrayList<String>(months.size)
    for ((index, bar) in months.withIndex()) {
        val name = monthOnly.format(bar.lastDay)
        val shown = if (index == 0 && name.isNotEmpty()) {
            name.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(locale) else char.toString()
            }
        } else {
            name
        }
        parts += "$shown ${groupThousands(bar.steps)}"
    }
    return stringResource(
        R.string.trends_chart_12_a11y,
        full.format(first.lastDay),
        full.format(months.last().lastDay),
        parts.joinToString(", "),
    )
}

@Composable
private fun chartLocale(): Locale {
    val locales = LocalConfiguration.current.locales
    return if (locales.isEmpty) Locale.getDefault() else locales[0]
}

private fun monthBarLabels(months: List<TrendMonthBar>): List<String> {
    val labels = ArrayList<String>(months.size)
    for ((index, bar) in months.withIndex()) {
        labels += if (showsTrendBarLabel(index, months.size)) {
            val month = bar.lastDay.monthValue
            if (month < 10) "0$month" else month.toString()
        } else {
            ""
        }
    }
    return labels
}

@Composable
private fun chartDescription(bars: List<TrendDayBar>, goals: List<Int>): String {
    val monthLabel = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    val parts = ArrayList<String>(bars.size)
    for (bar in bars) {
        val steps = groupThousands(bar.steps)
        val weekday = stringResource(fullWeekday(bar.day.dayOfWeek))
        parts += if (bars.size <= SevenDayCount) {
            "$weekday $steps"
        } else {
            "$weekday ${bar.day.dayOfMonth} ${monthLabel.format(bar.day)} $steps"
        }
    }
    val spoken = parts.joinToString(", ")
    val distinctGoals = ArrayList<Int>(goals.size)
    for (value in goals) {
        if (value !in distinctGoals) distinctGoals += value
    }
    if (distinctGoals.size <= 1) {
        val single = distinctGoals.firstOrNull() ?: 0
        return stringResource(
            R.string.trends_chart_days_a11y,
            bars.size,
            groupThousands(single.toLong()),
            spoken,
        )
    }
    val listed = distinctGoals.joinToString(", ") { groupThousands(it.toLong()) }
    return stringResource(
        R.string.trends_chart_days_goals_a11y,
        bars.size,
        listed,
        spoken,
    )
}

@StringRes
private fun chartAbbreviation(day: DayOfWeek): Int = when (day) {
    DayOfWeek.MONDAY -> R.string.chart_mon
    DayOfWeek.TUESDAY -> R.string.chart_tue
    DayOfWeek.WEDNESDAY -> R.string.chart_wed
    DayOfWeek.THURSDAY -> R.string.chart_thu
    DayOfWeek.FRIDAY -> R.string.chart_fri
    DayOfWeek.SATURDAY -> R.string.chart_sat
    DayOfWeek.SUNDAY -> R.string.chart_sun
}

@StringRes
private fun fullWeekday(day: DayOfWeek): Int = when (day) {
    DayOfWeek.MONDAY -> R.string.day_mon
    DayOfWeek.TUESDAY -> R.string.day_tue
    DayOfWeek.WEDNESDAY -> R.string.day_wed
    DayOfWeek.THURSDAY -> R.string.day_thu
    DayOfWeek.FRIDAY -> R.string.day_fri
    DayOfWeek.SATURDAY -> R.string.day_sat
    DayOfWeek.SUNDAY -> R.string.day_sun
}

private const val PeriodSevenDays = 0
private const val PeriodThirtyDays = 1
private const val PeriodTwelveMonths = 2
private const val SevenDayCount = 7
private const val ThirtyDayCount = 30

private const val FireGlyph = "\uE242"
private const val FootprintsGlyph = "\uEA88"
