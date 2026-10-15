package io.github.enterenn.steply.ui.trends

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import io.github.enterenn.steply.R
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.components.SteplyChartTick
import io.github.enterenn.steply.ui.components.SteplyHistogram
import io.github.enterenn.steply.ui.components.SteplySegmentedControl
import io.github.enterenn.steply.ui.components.SteplyStatTile
import io.github.enterenn.steply.ui.theme.SteplyTheme

@Composable
fun TrendsScreen() {
    val spacing = SteplyTheme.spacing
    var period by remember { mutableIntStateOf(PeriodSevenDays) }
    val month = period == PeriodThirtyDays
    val year = period == PeriodTwelveMonths

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
            SteplyHistogram(
                values = when {
                    year -> ExampleYearSteps
                    month -> ExampleMonthSteps
                    else -> ExampleWeekSteps
                },
                maxValue = if (year) ExampleYearMax else ExampleChartMax,
                goalValue = if (year) 0 else ExampleChartGoal,
                ticks = if (year) {
                    listOf(
                        SteplyChartTick(ExampleYearMax, stringResource(R.string.chart_axis_year_max)),
                        SteplyChartTick(ExampleChartGoal, stringResource(R.string.chart_axis_goal)),
                        SteplyChartTick(0, stringResource(R.string.chart_axis_zero)),
                    )
                } else {
                    listOf(
                        SteplyChartTick(ExampleChartMax, stringResource(R.string.chart_axis_max)),
                        SteplyChartTick(ExampleChartGoal, stringResource(R.string.chart_axis_goal)),
                        SteplyChartTick(0, stringResource(R.string.chart_axis_zero)),
                    )
                },
                dayLabels = when {
                    year -> ExampleYearLabels
                    month -> ExampleMonthLabels
                    else -> listOf(
                        stringResource(R.string.chart_fri),
                        stringResource(R.string.chart_sat),
                        stringResource(R.string.chart_sun),
                        stringResource(R.string.chart_mon),
                        stringResource(R.string.chart_tue),
                        stringResource(R.string.chart_wed),
                        stringResource(R.string.chart_thu),
                    )
                },
                contentDescription = stringResource(
                    when {
                        year -> R.string.trends_chart_12_a11y
                        month -> R.string.trends_chart_30_a11y
                        else -> R.string.trends_chart_a11y
                    },
                ),
            )
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
                value = when {
                    year -> ExampleYearStepsValue
                    month -> ExampleMonthStepsValue
                    else -> stringResource(R.string.gallery_stat_steps_value)
                },
                unit = stringResource(R.string.gallery_stat_steps_unit),
                caption = stringResource(R.string.gallery_stat_steps_caption),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val PeriodSevenDays = 0
private const val PeriodThirtyDays = 1
private const val PeriodTwelveMonths = 2

private val ExampleWeekSteps = listOf(0, 0, 0, 0, 1600, 2200, 1200)
private val ExampleMonthSteps = listOf(
    0, 400, 900, 1400, 1800, 2200, 1600,
    800, 200, 0, 600, 1100, 1700, 2400,
    2100, 1500, 900, 300, 0, 500, 1200,
    1900, 2600, 2000, 1300, 700, 200, 0,
    1100, 1800,
)
private val ExampleMonthLabels = List(ExampleMonthSteps.size) { index ->
    when (index) {
        0 -> "1"
        7 -> "8"
        14 -> "15"
        21 -> "22"
        28 -> "29"
        else -> ""
    }
}
private val ExampleYearSteps = listOf(
    0, 0, 0, 0, 0, 0, 0, 2400, 3400, 4200, 4000, 3600,
)
private val ExampleYearLabels = listOf(
    "11", "12", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10",
)
private const val ExampleChartMax = 2700
private const val ExampleChartGoal = 2000
private const val ExampleYearMax = 4300
private const val ExampleMonthKcal = "142"
private const val ExampleMonthStepsValue = "4180"
private const val ExampleYearKcal = "210"
private const val ExampleYearStepsValue = "3650"

private const val FireGlyph = "\uE242"
private const val FootprintsGlyph = "\uEA88"
