package io.github.enterenn.steply.ui.steps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.collectAsState
import io.github.enterenn.steply.R
import io.github.enterenn.steply.core.di.AppContainer
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.components.SteplyDayPill
import io.github.enterenn.steply.ui.components.SteplyPillButton
import io.github.enterenn.steply.ui.components.SteplyProgressRing
import io.github.enterenn.steply.ui.components.SteplyPillButtonStyle
import io.github.enterenn.steply.ui.theme.SteplyTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun StepsScreen(container: AppContainer) {
    val context = LocalContext.current
    val granted by container.activityRecognitionGranted.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        container.onAppForeground()
    }

    LaunchedEffect(granted) {
        if (
            !granted &&
            !container.activityRecognitionPromptShown &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            container.activityRecognitionPromptShown = true
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    val week = remember { currentWeek() }
    var selectedDay by rememberSaveable(key = "steps-week-day") { mutableIntStateOf(todayIndex()) }
    val spacing = SteplyTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.stack),
    ) {
        WeekDayCard(
            days = week,
            selectedIndex = selectedDay,
            onSelect = { selectedDay = it },
        )
        StepsProgressCard()
        if (!granted) {
            PermissionDeniedContent(
                onAllow = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                onOpenSettings = { openAppSettings(context) },
            )
        }
    }
}

@Composable
private fun WeekDayCard(
    days: List<WeekDay>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val spacing = SteplyTheme.spacing
    SteplyCard {
        BasicText(
            text = stringResource(R.string.steps_week_title),
            modifier = Modifier.semantics { heading() },
            style = SteplyTheme.typography.title.copy(color = SteplyTheme.colors.text),
        )
        Row(
            modifier = Modifier
                .padding(top = spacing.card)
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(SteplyTheme.spacing.inline / 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            days.forEachIndexed { index, day ->
                SteplyDayPill(
                    abbreviation = stringResource(day.shortName),
                    dayNumber = day.number,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    label = stringResource(
                        R.string.day_pill_label,
                        stringResource(day.fullName),
                        day.number,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StepsProgressCard() {
    SteplyCard {
        SteplyProgressRing(
            label = stringResource(R.string.steps_ring_label),
            valueText = stringResource(R.string.steps_ring_value),
            goalText = stringResource(R.string.steps_ring_goal),
            value = ExampleRingSteps,
            goal = ExampleRingGoal,
            contentDescription = stringResource(R.string.steps_ring_a11y),
        )
        Box(
            Modifier
                .padding(top = SteplyTheme.spacing.stack)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            SteplyPillButton(
                label = stringResource(R.string.steps_set_goal),
                onClick = {},
                style = SteplyPillButtonStyle.Secondary,
                verticalPadding = SetGoalButtonPadding,
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    val colors = SteplyTheme.colors
    val spacing = SteplyTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.stack)) {
        BasicText(
            text = stringResource(R.string.steps_permission_title),
            style = SteplyTheme.typography.title.copy(color = colors.text),
        )
        BasicText(
            text = stringResource(R.string.steps_permission_body),
            style = SteplyTheme.typography.body.copy(color = colors.text),
        )
        SteplyPillButton(
            label = stringResource(R.string.steps_permission_allow),
            onClick = onAllow,
            modifier = Modifier.fillMaxWidth(),
        )
        SteplyPillButton(
            label = stringResource(R.string.steps_permission_settings),
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            style = SteplyPillButtonStyle.Secondary,
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

private data class WeekDay(
    @StringRes val shortName: Int,
    @StringRes val fullName: Int,
    val number: String,
)

private fun currentWeek(today: LocalDate = LocalDate.now()): List<WeekDay> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return WeekOrder.map { day ->
        val (shortName, fullName) = dayLabels(day)
        val date = monday.plusDays((day.value - DayOfWeek.MONDAY.value).toLong())
        WeekDay(shortName, fullName, date.dayOfMonth.toString())
    }
}

private fun todayIndex(today: LocalDate = LocalDate.now()): Int =
    today.dayOfWeek.value - DayOfWeek.MONDAY.value

private fun dayLabels(day: DayOfWeek): Pair<Int, Int> = when (day) {
    DayOfWeek.MONDAY -> R.string.day_mon_short to R.string.day_mon
    DayOfWeek.TUESDAY -> R.string.day_tue_short to R.string.day_tue
    DayOfWeek.WEDNESDAY -> R.string.day_wed_short to R.string.day_wed
    DayOfWeek.THURSDAY -> R.string.day_thu_short to R.string.day_thu
    DayOfWeek.FRIDAY -> R.string.day_fri_short to R.string.day_fri
    DayOfWeek.SATURDAY -> R.string.day_sat_short to R.string.day_sat
    DayOfWeek.SUNDAY -> R.string.day_sun_short to R.string.day_sun
}

private val SetGoalButtonPadding = 8.dp

private const val ExampleRingSteps = 3750
private const val ExampleRingGoal = 10000

private val WeekOrder = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY,
)
