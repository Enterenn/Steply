package io.github.enterenn.steply.ui.steps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.enterenn.steply.R
import io.github.enterenn.steply.domain.CollectionHealth
import io.github.enterenn.steply.domain.collectionHealth
import io.github.enterenn.steply.domain.daysReachingGoal
import io.github.enterenn.steply.domain.DistanceUnit
import io.github.enterenn.steply.domain.derivedActivity
import io.github.enterenn.steply.domain.kmToMi
import io.github.enterenn.steply.domain.DailyGoalMax
import io.github.enterenn.steply.domain.DailyGoalMin
import io.github.enterenn.steply.domain.acceptsDailyGoalChange
import io.github.enterenn.steply.domain.resolveDailyGoal
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.components.SteplyDayPill
import io.github.enterenn.steply.ui.components.SteplyPillButton
import io.github.enterenn.steply.ui.components.SteplyProgressRing
import io.github.enterenn.steply.ui.components.SteplyPillButtonStyle
import io.github.enterenn.steply.ui.theme.SteplyTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun StepsScreen(
    viewModelFactory: ViewModelProvider.Factory,
    onEditGoal: () -> Unit,
    onActivityPermissionResult: () -> Unit,
) {
    val viewModel: StepsViewModel = viewModel(factory = viewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val granted = state.activityRecognitionGranted
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        onActivityPermissionResult()
    }

    val week = remember { currentWeek() }
    var selectedDay by rememberSaveable(key = "steps-week-day") { mutableIntStateOf(todayIndex()) }
    val selectedDate = week[selectedDay].date
    val readings = state.readings
    val goal = state.goal
    val steps = state.steps
    val weekSteps = state.weekSteps
    LaunchedEffect(Unit) {
        viewModel.observeInputs()
    }
    LaunchedEffect(selectedDate, readings) {
        viewModel.loadSelectedDay(selectedDate)
    }
    LaunchedEffect(readings) {
        viewModel.loadWeek(week.map { day -> day.date })
    }
    val spacing = SteplyTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.stack),
    ) {
        BasicText(
            text = stringResource(R.string.tab_steps),
            modifier = Modifier.semantics { heading() },
            style = SteplyTheme.typography.body.copy(color = SteplyTheme.colors.gray),
        )
        WeekDayCard(
            days = week,
            selectedIndex = selectedDay,
            daysAtGoal = daysReachingGoal(weekSteps, goal),
            onSelect = { selectedDay = it },
        )
        StepsProgressCard(
            steps = steps,
            goal = goal,
            health = collectionHealth(
                permissionGranted = granted,
                status = readings.status,
                hasSnapshot = readings.snapshot != null,
            ),
            onEditGoal = onEditGoal,
        )
        DerivedStatsCard(
            steps,
            state.heightCm,
            state.weightKg,
            miles = state.distance == DistanceUnit.Mi,
        )
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
    daysAtGoal: Int,
    onSelect: (Int) -> Unit,
) {
    val spacing = SteplyTheme.spacing
    val countLabel = pluralStringResource(
        R.plurals.steps_week_goal_a11y,
        daysAtGoal,
        daysAtGoal,
        days.size,
    )
    SteplyCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = stringResource(R.string.steps_week_title),
                modifier = Modifier.semantics { heading() },
                style = SteplyTheme.typography.title.copy(color = SteplyTheme.colors.text),
            )
            BasicText(
                text = "$daysAtGoal/${days.size}",
                modifier = Modifier.clearAndSetSemantics { contentDescription = countLabel },
                style = SteplyTheme.typography.caption.copy(color = SteplyTheme.colors.text),
            )
        }
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
private fun StepsProgressCard(
    steps: Long,
    goal: Int,
    health: CollectionHealth,
    onEditGoal: () -> Unit,
) {
    val stepsText = formatStepCount(steps)
    val goalText = formatStepCount(goal.toLong())
    SteplyCard {
        if (health != CollectionHealth.Hidden) {
            CollectionHealthLine(health)
        }
        SteplyProgressRing(
            label = stringResource(R.string.steps_ring_label),
            valueText = stepsText,
            goalText = "/$goalText",
            value = steps.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
            goal = goal,
            contentDescription = stringResource(
                R.string.steps_ring_a11y,
                stepsText,
                goalText,
            ),
        )
        Box(
            Modifier
                .padding(top = SteplyTheme.spacing.stack)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            SteplyPillButton(
                label = stringResource(R.string.steps_set_goal),
                onClick = onEditGoal,
                style = SteplyPillButtonStyle.Secondary,
                verticalPadding = SetGoalButtonPadding,
            )
        }
    }
}

@Composable
internal fun GoalEditorSheet(
    goal: Int,
    stepsToday: Long,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(goal.toString()) }
    val parsed = draft.toIntOrNull()
    val valid = parsed != null && parsed in DailyGoalMin..DailyGoalMax
    val resolved = if (valid) resolveDailyGoal(parsed) else null
    val outOfRange = parsed != null && (
        parsed > DailyGoalMax ||
            (parsed < DailyGoalMin && draft.length >= DailyGoalMin.toString().length)
        )
    val belowToday = resolved != null &&
        !acceptsDailyGoalChange(goal, resolved, stepsToday)
    val canSave = resolved != null && resolved != goal && !belowToday
    val colors = SteplyTheme.colors
    val spacing = SteplyTheme.spacing
    val inputDescription = stringResource(R.string.steps_goal_input_a11y)
    val focus = remember { FocusRequester() }
    val scrim = remember { MutableInteractionSource() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = ScrimAlpha))
                .clickable(interactionSource = scrim, indication = null, onClick = onDismiss),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = SteplyTheme.radius.card, topEnd = SteplyTheme.radius.card))
                    .background(colors.card)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(horizontal = spacing.screen)
                    .padding(top = spacing.inline, bottom = spacing.card),
            ) {
                Box(
                    Modifier
                        .padding(vertical = spacing.inline)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(SheetHandleWidth)
                            .height(SheetHandleHeight)
                            .clip(RoundedCornerShape(SheetHandleHeight))
                            .background(colors.lightGray),
                    )
                }
                BasicText(
                    text = stringResource(R.string.steps_goal_title),
                    modifier = Modifier.semantics { heading() },
                    style = SteplyTheme.typography.title.copy(color = colors.text),
                )
                BasicTextField(
                    value = draft,
                    onValueChange = { raw -> draft = raw.filter(Char::isDigit).take(GoalDigitLimit) },
                    modifier = Modifier
                        .padding(top = spacing.card)
                        .fillMaxWidth()
                        .height(GoalFieldHeight)
                        .focusRequester(focus)
                        .semantics { contentDescription = inputDescription },
                    textStyle = SteplyTheme.typography.body.copy(color = colors.text),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (canSave) onSave(resolveDailyGoal(parsed)) },
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.accent),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .border(
                                    GoalFieldStroke,
                                    if (outOfRange || belowToday) colors.error else colors.gray,
                                    RoundedCornerShape(SteplyTheme.radius.control),
                                )
                                .padding(horizontal = spacing.card),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            inner()
                        }
                    },
                )
                if (outOfRange || belowToday) {
                    BasicText(
                        text = stringResource(
                            if (outOfRange) R.string.steps_goal_error else R.string.steps_goal_below_today,
                        ),
                        modifier = Modifier.padding(top = spacing.inline),
                        style = SteplyTheme.typography.caption.copy(color = colors.error),
                    )
                }
                SteplyPillButton(
                    label = stringResource(R.string.steps_goal_save),
                    onClick = { if (canSave) onSave(resolveDailyGoal(parsed)) },
                    modifier = Modifier
                        .padding(top = spacing.card)
                        .fillMaxWidth()
                        .graphicsLayer { alpha = if (canSave) 1f else DisabledSaveAlpha },
                )
                Box(
                    Modifier
                        .padding(top = spacing.inline)
                        .fillMaxWidth()
                        .heightIn(min = GoalFieldHeight)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = stringResource(R.string.steps_goal_cancel),
                        style = SteplyTheme.typography.body.copy(color = colors.gray),
                    )
                }
            }
        }
    }

@Composable
private fun DerivedStatsCard(steps: Long, heightCm: Int?, weightKg: Int?, miles: Boolean) {
    val height = heightCm?.toDouble()
    val weight = weightKg?.toDouble()
    val metrics = when {
        height != null && weight != null -> derivedActivity(steps, height, weight)
        height != null -> derivedActivity(steps, heightCm = height)
        weight != null -> derivedActivity(steps, weightKg = weight)
        else -> derivedActivity(steps)
    }
    val comma = LocalConfiguration.current.locales[0].language == "fr"
    val kcal = stringResource(R.string.steps_stat_kcal, formatStepCount(metrics.kilocalories.toLong()))
    val distanceAmount = if (miles) kmToMi(metrics.distanceKm) else metrics.distanceKm
    val distance = stringResource(
        if (miles) R.string.steps_stat_distance_mi else R.string.steps_stat_distance,
        formatDistanceNumber(distanceAmount, comma),
    )
    val duration = formatWalkingClock(metrics.walkingMinutes)
    SteplyCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DerivedStat(
                glyph = FireGlyph,
                value = kcal,
                label = stringResource(R.string.steps_stat_kcal_a11y, kcal),
                modifier = Modifier.weight(1f),
            )
            StatRule()
            DerivedStat(
                glyph = PinGlyph,
                value = distance,
                label = stringResource(R.string.steps_stat_distance_a11y, distance),
                modifier = Modifier.weight(1f),
            )
            StatRule()
            DerivedStat(
                glyph = ClockGlyph,
                value = duration,
                label = stringResource(R.string.steps_stat_duration_a11y, duration),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CollectionHealthLine(health: CollectionHealth) {
    val colors = SteplyTheme.colors
    val (label, dot) = when (health) {
        CollectionHealth.Hidden -> return
        CollectionHealth.SensorMissing -> R.string.steps_collection_sensor_missing to colors.error
        CollectionHealth.TimedOut -> R.string.steps_collection_timed_out to colors.error
        CollectionHealth.NoReading -> R.string.steps_collection_no_reading to colors.gray
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SteplyTheme.spacing.stack),
        horizontalArrangement = Arrangement.spacedBy(
            SteplyTheme.spacing.inline,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(CollectionDotSize)
                .background(dot, CircleShape)
                .clearAndSetSemantics {},
        )
        BasicText(
            text = stringResource(label),
            style = SteplyTheme.typography.caption.copy(color = colors.text),
        )
    }
}

@Composable
private fun DerivedStat(
    glyph: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SteplyTheme.spacing.stack),
    ) {
        BasicText(
            text = glyph,
            modifier = Modifier.clearAndSetSemantics {},
            style = TextStyle(
                fontFamily = PhosphorRegular,
                fontSize = StatGlyphSize,
                lineHeight = StatGlyphSize,
                color = SteplyTheme.colors.text,
            ),
        )
        BasicText(
            text = value,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            style = SteplyTheme.typography.caption.copy(
                color = SteplyTheme.colors.text,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun StatRule() {
    Box(
        Modifier
            .width(StatRuleWidth)
            .height(SteplyTheme.spacing.card)
            .background(SteplyTheme.colors.accent)
            .clearAndSetSemantics {},
    )
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
    val date: LocalDate,
)

private fun currentWeek(today: LocalDate = LocalDate.now()): List<WeekDay> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return WeekOrder.map { day ->
        val (shortName, fullName) = dayLabels(day)
        val date = monday.plusDays((day.value - DayOfWeek.MONDAY.value).toLong())
        WeekDay(shortName, fullName, date.dayOfMonth.toString(), date)
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
private val SheetHandleWidth = 36.dp
private val SheetHandleHeight = 4.dp
private val GoalFieldHeight = 56.dp
private val GoalFieldStroke = 1.dp
private const val GoalDigitLimit = 6
private const val DisabledSaveAlpha = 0.4f
private const val ScrimAlpha = 0.32f
private val StatGlyphSize = 20.sp
private val StatRuleWidth = 1.dp
private val CollectionDotSize = 8.dp

private fun formatStepCount(steps: Long): String {
    val digits = steps.coerceAtLeast(0L).toString()
    return digits.reversed().chunked(3).joinToString(" ").reversed()
}

private fun formatDistanceNumber(kilometers: Double, comma: Boolean): String {
    val text = String.format(Locale.US, "%.2f", kilometers.coerceAtLeast(0.0))
    return if (comma) text.replace('.', ',') else text
}

private fun formatWalkingClock(minutes: Double): String {
    val totalSeconds = (minutes.coerceAtLeast(0.0) * 60.0).roundToLong()
    val hours = totalSeconds / 3_600
    val mins = (totalSeconds % 3_600) / 60
    val secs = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
}

private const val FireGlyph = "\uE242"
private const val PinGlyph = "\uE316"
private const val ClockGlyph = "\uE19A"

private val PhosphorRegular = FontFamily(Font(R.font.phosphor_regular))

private val WeekOrder = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY,
)
