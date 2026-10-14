package io.github.enterenn.steply.ui.trends

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.enterenn.steply.R
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.components.SteplyChartTick
import io.github.enterenn.steply.ui.components.SteplyHistogram
import io.github.enterenn.steply.ui.components.SteplyListRow
import io.github.enterenn.steply.ui.components.SteplyStatTile
import io.github.enterenn.steply.ui.components.SteplyPillButton
import io.github.enterenn.steply.ui.components.SteplyPillButtonStyle
import io.github.enterenn.steply.ui.components.SteplySegmentedControl
import io.github.enterenn.steply.ui.components.SteplySwitch
import io.github.enterenn.steply.ui.theme.SteplyTheme

@Composable
fun TrendsScreen() {
    var segment by rememberSaveable { mutableIntStateOf(0) }
    var notifications by rememberSaveable { mutableStateOf(false) }
    val spacing = SteplyTheme.spacing
    val options = listOf(
        stringResource(R.string.gallery_segment_day),
        stringResource(R.string.gallery_segment_week),
        stringResource(R.string.gallery_segment_month),
    )

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
        GallerySection(R.string.gallery_card_title) {
            SteplyCard {
                BasicText(
                    text = stringResource(R.string.gallery_card_body),
                    style = SteplyTheme.typography.body.copy(color = SteplyTheme.colors.text),
                )
            }
        }
        GallerySection(R.string.gallery_list_title) {
            SteplyCard(contentPadding = PaddingValues()) {
                SteplyListRow(label = stringResource(R.string.gallery_list_profile))
                SteplyListRow(
                    label = stringResource(R.string.gallery_list_data),
                    value = stringResource(R.string.gallery_list_data_value),
                )
                SteplyListRow(label = stringResource(R.string.gallery_list_about))
            }
        }
        GallerySection(R.string.gallery_segment_title) {
            SteplySegmentedControl(
                options = options,
                selectedIndex = segment,
                onSelect = { segment = it },
            )
        }
        GallerySection(R.string.gallery_button_title) {
            SteplyPillButton(
                label = stringResource(R.string.gallery_button_primary),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            SteplyPillButton(
                label = stringResource(R.string.gallery_button_secondary),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                style = SteplyPillButtonStyle.Secondary,
            )
            SteplyPillButton(
                label = stringResource(R.string.gallery_button_destructive),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                style = SteplyPillButtonStyle.DestructiveText,
            )
        }
        GallerySection(R.string.gallery_switch_title) {
            SteplySwitch(
                checked = notifications,
                onCheckedChange = { notifications = it },
                label = stringResource(R.string.gallery_switch_label),
            )
        }
    }
}

@Composable
private fun GallerySection(
    @StringRes title: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteplyTheme.spacing.inline),
    ) {
        BasicText(
            text = stringResource(title),
            modifier = Modifier.semantics { heading() },
            style = SteplyTheme.typography.body.copy(color = SteplyTheme.colors.text),
        )
        content()
    }
}

private val ExampleWeekSteps = listOf(0, 0, 0, 0, 1600, 2200, 1200)
private const val ExampleChartMax = 2700
private const val ExampleChartGoal = 2000

private const val FireGlyph = "\uE242"
private const val FootprintsGlyph = "\uEA88"
