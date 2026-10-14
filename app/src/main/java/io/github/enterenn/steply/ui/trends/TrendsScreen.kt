package io.github.enterenn.steply.ui.trends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.enterenn.steply.R
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.components.SteplyChartTick
import io.github.enterenn.steply.ui.components.SteplyHistogram
import io.github.enterenn.steply.ui.components.SteplyStatTile
import io.github.enterenn.steply.ui.theme.SteplyTheme

@Composable
fun TrendsScreen() {
    val spacing = SteplyTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.stack),
    ) {
        SteplyCard {
            SteplyHistogram(
                values = ExampleWeekSteps,
                maxValue = ExampleChartMax,
                goalValue = ExampleChartGoal,
                ticks = listOf(
                    SteplyChartTick(ExampleChartMax, stringResource(R.string.chart_axis_max)),
                    SteplyChartTick(ExampleChartGoal, stringResource(R.string.chart_axis_goal)),
                    SteplyChartTick(0, stringResource(R.string.chart_axis_zero)),
                ),
                dayLabels = listOf(
                    stringResource(R.string.chart_fri),
                    stringResource(R.string.chart_sat),
                    stringResource(R.string.chart_sun),
                    stringResource(R.string.chart_mon),
                    stringResource(R.string.chart_tue),
                    stringResource(R.string.chart_wed),
                    stringResource(R.string.chart_thu),
                ),
                contentDescription = stringResource(R.string.trends_chart_a11y),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.stack),
        ) {
            SteplyStatTile(
                glyph = FireGlyph,
                value = stringResource(R.string.gallery_stat_kcal_value),
                unit = stringResource(R.string.gallery_stat_kcal_unit),
                caption = stringResource(R.string.gallery_stat_kcal_caption),
                modifier = Modifier.weight(1f),
            )
            SteplyStatTile(
                glyph = FootprintsGlyph,
                value = stringResource(R.string.gallery_stat_steps_value),
                unit = stringResource(R.string.gallery_stat_steps_unit),
                caption = stringResource(R.string.gallery_stat_steps_caption),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val ExampleWeekSteps = listOf(0, 0, 0, 0, 1600, 2200, 1200)
private const val ExampleChartMax = 2700
private const val ExampleChartGoal = 2000

private const val FireGlyph = "\uE242"
private const val FootprintsGlyph = "\uEA88"
