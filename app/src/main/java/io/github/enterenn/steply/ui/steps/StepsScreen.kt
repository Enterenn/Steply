package io.github.enterenn.steply.ui.steps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.collectAsState
import io.github.enterenn.steply.R
import io.github.enterenn.steply.core.di.AppContainer
import io.github.enterenn.steply.ui.components.SteplyPillButton
import io.github.enterenn.steply.ui.components.SteplyPillButtonStyle
import io.github.enterenn.steply.data.steps.StepCounterSnapshot
import io.github.enterenn.steply.data.steps.StepReadStatus
import io.github.enterenn.steply.ui.theme.SteplyTheme
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun StepsScreen(container: AppContainer) {
    val context = LocalContext.current
    val granted by container.activityRecognitionGranted.collectAsState()
    val readings by container.stepReadings.collectAsState()
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

    if (!granted) {
        PermissionDeniedContent(
            onAllow = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
            onOpenSettings = { openAppSettings(context) },
        )
        return
    }

    ReadingContent(
        snapshot = readings.snapshot,
        status = readings.status,
    )
}

@Composable
private fun ReadingContent(snapshot: StepCounterSnapshot?, status: StepReadStatus) {
    val colors = SteplyTheme.colors
    val spacing = SteplyTheme.spacing
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.stack),
    ) {
        BasicText(
            text = stringResource(R.string.steps_counter_label),
            style = SteplyTheme.typography.caption.copy(color = colors.gray),
        )
        BasicText(
            text = snapshot?.let { numberFormat.format(it.cumulativeSteps) }
                ?: stringResource(R.string.steps_waiting),
            style = if (snapshot == null) {
                SteplyTheme.typography.body.copy(color = colors.text)
            } else {
                SteplyTheme.typography.display.copy(color = colors.text)
            },
        )
        if (snapshot != null) {
            BasicText(
                text = stringResource(
                    R.string.steps_read_time,
                    formatRecordedAt(snapshot.recordedAt, snapshot.zoneOffset),
                ),
                style = SteplyTheme.typography.body.copy(color = colors.text),
            )
            BasicText(
                text = deltaLabel(snapshot, numberFormat),
                style = SteplyTheme.typography.body.copy(color = colors.text),
            )
        }
        BasicText(
            text = stringResource(R.string.steps_counter_hint),
            style = SteplyTheme.typography.caption.copy(color = colors.gray),
        )
        if (status != StepReadStatus.Idle) {
            BasicText(
                text = stringResource(
                    if (status == StepReadStatus.SensorMissing) {
                        R.string.steps_sensor_missing
                    } else {
                        R.string.steps_sensor_timeout
                    },
                ),
                style = SteplyTheme.typography.caption.copy(color = colors.error),
            )
        }
    }
}

@Composable
private fun deltaLabel(snapshot: StepCounterSnapshot, numberFormat: NumberFormat): String {
    val steps = snapshot.deltaSteps ?: return stringResource(R.string.steps_delta_none)
    val formatted = numberFormat.format(steps)
    return if (snapshot.counterReset) {
        stringResource(R.string.steps_delta_reset, formatted)
    } else {
        stringResource(R.string.steps_delta, formatted)
    }
}

@Composable
private fun PermissionDeniedContent(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    val colors = SteplyTheme.colors
    val spacing = SteplyTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.stack, Alignment.CenterVertically),
    ) {
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

private fun formatRecordedAt(recordedAt: String, zoneOffset: String): String {
    return try {
        val local = Instant.parse(recordedAt).atOffset(ZoneOffset.of(zoneOffset))
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .format(local)
    } catch (_: DateTimeParseException) {
        recordedAt
    } catch (_: java.time.DateTimeException) {
        recordedAt
    }
}
