package io.github.enterenn.steply.ui.menu

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.enterenn.steply.R
import io.github.enterenn.steply.core.di.AppContainer
import io.github.enterenn.steply.data.steps.AppLanguage
import io.github.enterenn.steply.data.steps.DistanceUnit
import io.github.enterenn.steply.data.steps.HeightUnit
import io.github.enterenn.steply.data.steps.Settings
import io.github.enterenn.steply.data.steps.ThemeMode
import io.github.enterenn.steply.data.steps.WeightUnit
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.components.SteplySegmentedControl
import io.github.enterenn.steply.ui.components.SteplySwitch
import io.github.enterenn.steply.ui.theme.AccentPreset
import io.github.enterenn.steply.ui.theme.SteplyTheme

private val PhosphorRegular = FontFamily(Font(R.font.phosphor_regular))
private const val ArrowLeftGlyph = "\uE058"
private const val CaretRightGlyph = "\uE13A"
private const val CheckGlyph = "\uE182"
private val BackTarget = 48.dp
private val ChipTouch = 48.dp
private val ChipSize = 32.dp
private val ChipRing = 1.dp
private val ChipRingSelected = 3.dp
private const val ScrimAlpha = 0.32f

private val AccentOrder = listOf(
    AccentPreset.Orange,
    AccentPreset.Red,
    AccentPreset.Green,
    AccentPreset.Blue,
    AccentPreset.Violet,
    AccentPreset.Pink,
)

private enum class PickerKind { Language, Distance, Weight, Height }

@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val settings by container.settings.collectAsState(initial = Settings())
    val activity = LocalContext.current.findActivity()
    var picker by remember { mutableStateOf<PickerKind?>(null) }
    BackHandler {
        if (picker != null) picker = null else onBack()
    }
    val spacing = SteplyTheme.spacing
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .submenuPadding(),
            verticalArrangement = Arrangement.spacedBy(spacing.stack),
        ) {
            SettingsTitle(onBack)
            LanguageCard(settings, onOpen = { picker = PickerKind.Language })
            UnitsCard(settings, onOpen = { picker = it })
            NotificationsCard(settings.goalNotifications, container)
            ThemeCard(settings, container)
        }
        val open = picker
        if (open != null) {
            OptionSheet(
                title = stringResource(pickerTitle(open)),
                options = pickerOptions(open),
                selectedIndex = pickerSelection(open, settings),
                onSelect = { index ->
                    picker = null
                    applyPicker(open, index, settings, container, activity)
                },
                onDismiss = { picker = null },
            )
        }
    }
}

@Composable
private fun SettingsTitle(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteplyTheme.spacing.inline),
    ) {
        SettingsBackButton(onClick = onBack)
        BasicText(
            text = stringResource(R.string.menu_settings),
            modifier = Modifier.semantics { heading() },
            style = SteplyTheme.typography.body.copy(color = SteplyTheme.colors.gray),
        )
    }
}

@Composable
private fun LanguageCard(settings: Settings, onOpen: () -> Unit) {
    SteplyCard {
        GroupTitle(R.string.settings_language)
        PreferenceRow(
            label = languageValue(settings.language),
            value = "",
            onClick = onOpen,
            modifier = Modifier.padding(top = SteplyTheme.spacing.card),
        )
    }
}

@Composable
private fun UnitsCard(settings: Settings, onOpen: (PickerKind) -> Unit) {
    val spacing = SteplyTheme.spacing
    SteplyCard {
        GroupTitle(R.string.settings_units)
        PreferenceRow(
            label = stringResource(R.string.settings_distance),
            value = stringResource(
                if (settings.distance == DistanceUnit.Mi) R.string.settings_unit_imperial
                else R.string.settings_unit_metric,
            ),
            onClick = { onOpen(PickerKind.Distance) },
            modifier = Modifier.padding(top = spacing.card),
        )
        PreferenceRow(
            label = stringResource(R.string.settings_weight),
            value = stringResource(
                if (settings.weight == WeightUnit.Lb) R.string.onboarding_unit_lb
                else R.string.settings_unit_kg,
            ),
            onClick = { onOpen(PickerKind.Weight) },
        )
        PreferenceRow(
            label = stringResource(R.string.settings_height),
            value = stringResource(
                if (settings.height == HeightUnit.In) R.string.onboarding_unit_in
                else R.string.onboarding_unit_cm,
            ),
            onClick = { onOpen(PickerKind.Height) },
        )
    }
}

@Composable
private fun NotificationsCard(enabled: Boolean, container: AppContainer) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) container.setGoalNotifications(true)
    }
    SteplyCard {
        GroupTitle(R.string.settings_notifications)
        SteplySwitch(
            checked = enabled,
            onCheckedChange = { want ->
                if (!want) {
                    container.setGoalNotifications(false)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) container.setGoalNotifications(true) else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    container.setGoalNotifications(true)
                }
            },
            label = stringResource(R.string.settings_goal_notifications),
            modifier = Modifier.padding(top = SteplyTheme.spacing.card),
        )
    }
}

@Composable
private fun ThemeCard(settings: Settings, container: AppContainer) {
    val spacing = SteplyTheme.spacing
    val colors = SteplyTheme.colors
    SteplyCard {
        GroupTitle(R.string.settings_theme)
        SteplySegmentedControl(
            options = listOf(
                stringResource(R.string.settings_theme_system),
                stringResource(R.string.settings_theme_light),
                stringResource(R.string.settings_theme_dark),
            ),
            selectedIndex = when (settings.theme) {
                ThemeMode.System -> 0
                ThemeMode.Light -> 1
                ThemeMode.Dark -> 2
            },
            onSelect = { index ->
                val mode = when (index) {
                    1 -> ThemeMode.Light
                    2 -> ThemeMode.Dark
                    else -> ThemeMode.System
                }
                container.setThemeMode(mode)
            },
            modifier = Modifier.padding(top = spacing.card),
        )
        Row(
            modifier = Modifier
                .padding(top = spacing.card)
                .fillMaxWidth(),
        ) {
            AccentOrder.forEach { preset ->
                AccentSwatch(
                    preset = preset,
                    selected = preset == settings.accent,
                    base = colors.card,
                    onClick = { if (preset != settings.accent) container.setAccent(preset) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PreferenceRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SteplyTheme.colors
    val hint = stringResource(R.string.settings_change_a11y)
    val description = if (value.isEmpty()) "$label. $hint" else "$label, $value. $hint"
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = BackTarget)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            modifier = Modifier.weight(1f).clearAndSetSemantics {},
            style = SteplyTheme.typography.body.copy(color = colors.text),
        )
        if (value.isNotEmpty()) {
            BasicText(
                text = value,
                modifier = Modifier.clearAndSetSemantics {},
                style = SteplyTheme.typography.body.copy(color = colors.gray),
            )
        }
        BasicText(
            text = CaretRightGlyph,
            modifier = Modifier
                .padding(start = SteplyTheme.spacing.inline / 2)
                .clearAndSetSemantics {},
            style = TextStyle(
                fontFamily = PhosphorRegular,
                fontSize = SteplyTheme.typography.body.fontSize,
                color = colors.gray,
            ),
        )
    }
}

@Composable
private fun AccentSwatch(
    preset: AccentPreset,
    selected: Boolean,
    base: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(accentLabel(preset))
    val colors = SteplyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val ring = if (selected) ChipRingSelected else ChipRing
    Box(
        modifier
            .height(ChipTouch)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(ChipSize)
                .border(ring, if (selected) colors.accent else colors.gray, CircleShape)
                .padding(ring)
                .clip(CircleShape),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val basePath = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, 0f)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(basePath, base)
                val accentPath = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, 0f)
                    close()
                }
                drawPath(accentPath, preset.light)
            }
        }
    }
}

@Composable
private fun OptionSheet(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SteplyTheme.colors
    val spacing = SteplyTheme.spacing
    val scrim = remember { MutableInteractionSource() }
    val sheet = remember { MutableInteractionSource() }
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
                .navigationBarsPadding()
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = SteplyTheme.radius.card, topEnd = SteplyTheme.radius.card))
                .background(colors.card)
                .clickable(interactionSource = sheet, indication = null, onClick = {})
                .padding(horizontal = spacing.screen, vertical = spacing.card),
        ) {
            BasicText(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = SteplyTheme.typography.title.copy(color = colors.text),
            )
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                Row(
                    Modifier
                        .padding(top = spacing.inline)
                        .fillMaxWidth()
                        .heightIn(min = BackTarget)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClick = { onSelect(index) },
                        )
                        .semantics { this.selected = selected }
                        .padding(horizontal = spacing.inline),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = SteplyTheme.typography.body.copy(
                            color = if (selected) colors.accent else colors.text,
                            fontWeight = if (selected) FontWeight.W600 else FontWeight.W400,
                        ),
                    )
                    if (selected) {
                        BasicText(
                            text = CheckGlyph,
                            modifier = Modifier.clearAndSetSemantics {},
                            style = TextStyle(
                                fontFamily = PhosphorRegular,
                                fontSize = 20.sp,
                                color = colors.accent,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun pickerOptions(kind: PickerKind): List<String> = when (kind) {
    PickerKind.Language -> listOf(
        stringResource(R.string.settings_language_device),
        stringResource(R.string.settings_language_en),
        stringResource(R.string.settings_language_fr),
    )
    PickerKind.Distance -> listOf(
        stringResource(R.string.settings_unit_metric),
        stringResource(R.string.settings_unit_imperial),
    )
    PickerKind.Weight -> listOf(
        stringResource(R.string.settings_unit_kg),
        stringResource(R.string.onboarding_unit_lb),
    )
    PickerKind.Height -> listOf(
        stringResource(R.string.onboarding_unit_cm),
        stringResource(R.string.onboarding_unit_in),
    )
}

@Composable
private fun languageValue(language: AppLanguage): String = when (language) {
    AppLanguage.English -> stringResource(R.string.settings_language_en)
    AppLanguage.French -> stringResource(R.string.settings_language_fr)
    AppLanguage.System -> stringResource(R.string.settings_language_device)
}

@StringRes
private fun pickerTitle(kind: PickerKind): Int = when (kind) {
    PickerKind.Language -> R.string.settings_language
    PickerKind.Distance -> R.string.settings_distance
    PickerKind.Weight -> R.string.settings_weight
    PickerKind.Height -> R.string.settings_height
}

private fun pickerSelection(kind: PickerKind, settings: Settings): Int = when (kind) {
    PickerKind.Language -> when (settings.language) {
        AppLanguage.English -> 1
        AppLanguage.French -> 2
        AppLanguage.System -> 0
    }
    PickerKind.Distance -> if (settings.distance == DistanceUnit.Mi) 1 else 0
    PickerKind.Weight -> if (settings.weight == WeightUnit.Lb) 1 else 0
    PickerKind.Height -> if (settings.height == HeightUnit.In) 1 else 0
}

private fun applyPicker(
    kind: PickerKind,
    index: Int,
    settings: Settings,
    container: AppContainer,
    activity: Activity?,
) {
    when (kind) {
        PickerKind.Language -> {
            val next = when (index) {
                1 -> AppLanguage.English
                2 -> AppLanguage.French
                else -> AppLanguage.System
            }
            if (next != settings.language) container.setLanguage(next) { activity?.recreate() }
        }
        PickerKind.Distance -> {
            val next = if (index == 1) DistanceUnit.Mi else DistanceUnit.Km
            if (next != settings.distance) container.setDistanceUnit(next)
        }
        PickerKind.Weight -> {
            val next = if (index == 1) WeightUnit.Lb else WeightUnit.Kg
            if (next != settings.weight) container.setWeightUnit(next)
        }
        PickerKind.Height -> {
            val next = if (index == 1) HeightUnit.In else HeightUnit.Cm
            if (next != settings.height) container.setHeightUnit(next)
        }
    }
}

@Composable
private fun GroupTitle(@StringRes title: Int) {
    BasicText(
        text = stringResource(title),
        modifier = Modifier.semantics { heading() },
        style = SteplyTheme.typography.title.copy(color = SteplyTheme.colors.text),
    )
}

@StringRes
private fun accentLabel(preset: AccentPreset): Int = when (preset) {
    AccentPreset.Red -> R.string.settings_accent_red
    AccentPreset.Orange -> R.string.settings_accent_orange
    AccentPreset.Green -> R.string.settings_accent_green
    AccentPreset.Blue -> R.string.settings_accent_blue
    AccentPreset.Violet -> R.string.settings_accent_violet
    AccentPreset.Pink -> R.string.settings_accent_pink
}

@Composable
private fun SettingsBackButton(onClick: () -> Unit) {
    val label = stringResource(R.string.settings_back)
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

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
