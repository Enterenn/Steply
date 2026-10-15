package io.github.enterenn.steply.ui.trends

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import io.github.enterenn.steply.R
import io.github.enterenn.steply.core.di.AppContainer
import io.github.enterenn.steply.data.steps.TrendDayBar
import io.github.enterenn.steply.data.steps.TrendGoalSteps
import io.github.enterenn.steply.data.steps.averageTrendSteps
import io.github.enterenn.steply.data.steps.formatTrendAxisTick
import io.github.enterenn.steply.data.steps.groupThousands
import io.github.enterenn.steply.data.steps.showsTrendBarLabel
import io.github.enterenn.steply.data.steps.trendAxisTickValues
import io.github.enterenn.steply.data.steps.trendChartMax
import io.github.enterenn.steply.data.steps.trendDayBars
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.components.SteplyChartTick
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
    val dayCount = if (month) ThirtyDayCount else SevenDayCount
    val readings by container.stepReadings.collectAsState()
    var bars by remember { mutableStateOf<List<TrendDayBar>>(emptyList()) }
    LaunchedEffect(dayCount, readings, year) {
        if (year) return@LaunchedEffect
        bars = container.stepCounterRepository.trendBars(dayCount)
    }
    val chartBars = when {
        year -> emptyList()
        bars.size == dayCount -> bars
        else -> trendDayBars(emptyList(), LocalDate.now(), dayCount, ZoneId.systemDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.stack),
    ) {
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
                text = stringResource(R.string.trends_range_12),
                modifier = Modifier
                    .clip(RoundedCornerShape(SteplyTheme.radius.control))
                    .background(SteplyTheme.colors.lightGray)
                    .padding(horizontal = spacing.card, vertical = spacing.inline / 2),
                style = SteplyTheme.typography.caption.copy(color = SteplyTheme.colors.text),
            )
        }
        SteplyCard {
            if (year) {
                SteplyHistogram(
                    values = ExampleYearSteps,
                    maxValue = ExampleYearMax,
                    goalValue = 0,
                    ticks = listOf(
                        SteplyChartTick(ExampleYearMax, stringResource(R.string.chart_axis_year_max)),
                        SteplyChartTick(ExampleChartGoal, stringResource(R.string.chart_axis_goal)),
                        SteplyChartTick(0, stringResource(R.string.chart_axis_zero)),
                    ),
                    dayLabels = ExampleYearLabels,
                    contentDescription = stringResource(R.string.trends_chart_12_a11y),
                )
            } else {
                val maxValue = trendChartMax(chartBars.maxOfOrNull { it.steps } ?: 0L)
                SteplyHistogram(
                    values = chartBars.map { bar ->
                        bar.steps.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                    },
                    maxValue = maxValue,
                    goalValue = TrendGoalSteps,
                    ticks = trendAxisTickValues(maxValue).map { value ->
                        SteplyChartTick(value, formatTrendAxisTick(value))
                    },
                    dayLabels = chartDayLabels(chartBars),
                    contentDescription = chartDescription(chartBars),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.stack),
        ) {
            SteplyStatTile(
                glyph = FireGlyph,
                value = when {
                    year -> ExampleYearKcal
                    month -> ExampleMonthKcal
                    else -> stringResource(R.string.gallery_stat_kcal_value)
                },
                unit = stringResource(R.string.gallery_stat_kcal_unit),
                caption = stringResource(R.string.gallery_stat_kcal_caption),
                modifier = Modifier.weight(1f),
            )
            SteplyStatTile(
                glyph = FootprintsGlyph,
                value = if (year) {
                    ExampleYearStepsValue
                } else {
                    groupThousands(averageTrendSteps(chartBars).toLong())
                },
                unit = stringResource(R.string.gallery_stat_steps_unit),
                caption = stringResource(R.string.gallery_stat_steps_caption),
                modifier = Modifier.weight(1f),
            )
        }
    }
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

@Composable
private fun chartDescription(bars: List<TrendDayBar>): String {
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
    return stringResource(
        R.string.trends_chart_days_a11y,
        bars.size,
        groupThousands(TrendGoalSteps.toLong()),
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

private val ExampleYearSteps = listOf(
    0, 0, 0, 0, 0, 0, 0, 2400, 3400, 4200, 4000, 3600,
)
private val ExampleYearLabels = listOf(
    "11", "12", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10",
)
private const val ExampleChartGoal = 2000
private const val ExampleYearMax = 4300
private const val ExampleMonthKcal = "142"
private const val ExampleYearKcal = "210"
private const val ExampleYearStepsValue = "3650"

private const val FireGlyph = "\uE242"
private const val FootprintsGlyph = "\uEA88"
