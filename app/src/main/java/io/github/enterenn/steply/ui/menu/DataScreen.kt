package io.github.enterenn.steply.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.enterenn.steply.R
import io.github.enterenn.steply.core.di.AppContainer
import io.github.enterenn.steply.data.db.AppDatabase
import io.github.enterenn.steply.data.steps.CollectionHealth
import io.github.enterenn.steply.data.steps.StepReadingsState
import io.github.enterenn.steply.data.steps.collectionHealth
import io.github.enterenn.steply.data.steps.formatStorageSize
import io.github.enterenn.steply.data.steps.storageFileBytes
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.theme.SteplyTheme
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PhosphorRegular = FontFamily(Font(R.font.phosphor_regular))
private const val ArrowLeftGlyph = "\uE058"
private val BackTarget = 48.dp
private val StatusDot = 8.dp
private val StatusDotNudge = 7.dp
private val KpiGap = 4.dp

@Composable
fun DataScreen(container: AppContainer, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val granted by container.activityRecognitionGranted.collectAsState()
    val readings by container.stepReadings.collectAsState()
    val context = LocalContext.current.applicationContext
    var readingCount by remember { mutableLongStateOf(0L) }
    var databaseBytes by remember { mutableLongStateOf(0L) }
    LaunchedEffect(readings) {
        readingCount = container.stepCounterRepository.readingCount()
        databaseBytes = withContext(Dispatchers.IO) {
            val db = context.getDatabasePath(AppDatabase.NAME)
            storageFileBytes(
                fileLength(db),
                fileLength(File(db.path + "-wal")),
                fileLength(File(db.path + "-shm")),
            )
        }
    }
    val locales = LocalConfiguration.current.locales
    val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
    val spacing = SteplyTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.stack),
    ) {
        DataTitle(stringResource(R.string.menu_data), onBack)
        DataCard(title = stringResource(R.string.data_background)) {
            CollectionStateLine(granted, readings)
            BasicText(
                text = stringResource(
                    R.string.data_last_reading_line,
                    lastReadingText(readings.snapshot?.recordedAt),
                ),
                style = SteplyTheme.typography.caption.copy(
                    color = SteplyTheme.colors.gray,
                    fontWeight = FontWeight.W400,
                ),
            )
        }
        DataCard(title = stringResource(R.string.data_footprint)) {
            FootprintKpi(
                value = groupedCount(readingCount),
                label = stringResource(R.string.data_readings),
            )
            FootprintKpi(
                value = formatStorageSize(databaseBytes, locale),
                label = stringResource(R.string.data_database),
            )
        }
    }
}

@Composable
private fun collectionStateText(granted: Boolean, readings: StepReadingsState): String {
    val health = collectionHealth(
        permissionGranted = granted,
        status = readings.status,
        hasSnapshot = readings.snapshot != null,
    )
    return when {
        !granted -> stringResource(R.string.data_state_off)
        health == CollectionHealth.SensorMissing -> stringResource(R.string.steps_collection_sensor_missing)
        health == CollectionHealth.TimedOut -> stringResource(R.string.steps_collection_timed_out)
        health == CollectionHealth.NoReading -> stringResource(R.string.steps_collection_no_reading)
        else -> stringResource(R.string.data_state_collecting)
    }
}

@Composable
private fun lastReadingText(recordedAt: String?): String {
    if (recordedAt == null) return stringResource(R.string.data_not_set)
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .format(Instant.parse(recordedAt).atZone(ZoneId.systemDefault()))
}

@Composable
private fun CollectionStateLine(granted: Boolean, readings: StepReadingsState) {
    val colors = SteplyTheme.colors
    val health = collectionHealth(
        permissionGranted = granted,
        status = readings.status,
        hasSnapshot = readings.snapshot != null,
    )
    val dot = when {
        !granted || health == CollectionHealth.NoReading -> colors.gray
        health == CollectionHealth.SensorMissing || health == CollectionHealth.TimedOut -> colors.error
        else -> colors.success
    }
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(SteplyTheme.spacing.inline),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = StatusDotNudge)
                .size(StatusDot)
                .background(dot, CircleShape)
                .clearAndSetSemantics {},
        )
        BasicText(
            text = collectionStateText(granted, readings),
            style = SteplyTheme.typography.body.copy(color = colors.text),
        )
    }
}

@Composable
private fun FootprintKpi(value: String, label: String) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(KpiGap),
    ) {
        BasicText(
            text = value,
            style = TextStyle(
                fontFamily = SteplyTheme.typography.figures,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.W500,
                color = SteplyTheme.colors.text,
            ),
        )
        BasicText(
            text = label,
            style = SteplyTheme.typography.caption.copy(
                color = SteplyTheme.colors.gray,
                fontWeight = FontWeight.W400,
            ),
        )
    }
}

@Composable
private fun DataCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    SteplyCard {
        Column(verticalArrangement = Arrangement.spacedBy(SteplyTheme.spacing.card)) {
            BasicText(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = SteplyTheme.typography.title.copy(color = SteplyTheme.colors.text),
            )
            content()
        }
    }
}

@Composable
private fun DataTitle(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteplyTheme.spacing.inline),
    ) {
        DataBackButton(onClick = onBack)
        BasicText(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = SteplyTheme.typography.body.copy(color = SteplyTheme.colors.gray),
        )
    }
}

@Composable
private fun DataBackButton(onClick: () -> Unit) {
    val label = stringResource(R.string.data_back)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(BackTarget)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = ArrowLeftGlyph,
            modifier = Modifier.clearAndSetSemantics {},
            style = TextStyle(
                fontFamily = PhosphorRegular,
                fontSize = SteplyTheme.typography.body.fontSize,
                color = SteplyTheme.colors.gray,
            ),
        )
    }
}

private fun fileLength(file: File): Long = if (file.isFile) file.length() else 0L

private fun groupedCount(count: Long): String {
    val digits = count.coerceAtLeast(0L).toString()
    return digits.reversed().chunked(3).joinToString(" ").reversed()
}
