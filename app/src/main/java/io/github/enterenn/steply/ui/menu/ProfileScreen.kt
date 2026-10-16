package io.github.enterenn.steply.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.enterenn.steply.R
import io.github.enterenn.steply.core.di.AppContainer
import io.github.enterenn.steply.domain.Profile
import io.github.enterenn.steply.domain.ProfileHeightMaxCm
import io.github.enterenn.steply.domain.ProfileHeightMinCm
import io.github.enterenn.steply.domain.ProfileMeasureMajorEvery
import io.github.enterenn.steply.domain.ProfileMeasureStep
import io.github.enterenn.steply.domain.ProfileNameMaxLength
import io.github.enterenn.steply.domain.ProfileWeightMaxKg
import io.github.enterenn.steply.domain.ProfileWeightMinKg
import io.github.enterenn.steply.domain.HeightUnit
import io.github.enterenn.steply.data.steps.Settings
import io.github.enterenn.steply.domain.WeightUnit
import io.github.enterenn.steply.domain.cmToIn
import io.github.enterenn.steply.domain.inchesToCm
import io.github.enterenn.steply.domain.kgToLb
import io.github.enterenn.steply.domain.lbToKg
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.components.SteplyListRow
import io.github.enterenn.steply.ui.components.SteplyPillButton
import io.github.enterenn.steply.ui.components.SteplyRuler
import io.github.enterenn.steply.ui.theme.SteplyTheme

private val PhosphorRegular = FontFamily(Font(R.font.phosphor_regular))
private const val ArrowLeftGlyph = "\uE058"
private val BackTarget = 48.dp
private val FieldMinHeight = 48.dp
private val FieldStroke = 1.dp
private const val EditName = "name"
private const val EditWeight = "weight"
private const val EditHeight = "height"
private const val InchMajorEvery = 12

@Composable
fun ProfileScreen(container: AppContainer, onBack: () -> Unit) {
    val profile by container.profile.collectAsState(initial = Profile())
    val settings by container.settings.collectAsState(initial = Settings())
    val weightLb = settings.weight == WeightUnit.Lb
    val heightIn = settings.height == HeightUnit.In
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    when (editing) {
        EditName -> NameEditor(
            profile = profile,
            onSave = { raw ->
                container.setProfileName(raw)
                editing = null
            },
            onCancel = { editing = null },
        )
        EditWeight -> MeasureEditor(
            title = stringResource(R.string.profile_weight),
            unit = stringResource(if (weightLb) R.string.onboarding_unit_lb else R.string.profile_unit_kg),
            value = profile.weightKg?.let { if (weightLb) kgToLb(it) else it }
                ?: if (weightLb) kgToLb(ProfileWeightMinKg) else ProfileWeightMinKg,
            min = if (weightLb) kgToLb(ProfileWeightMinKg) else ProfileWeightMinKg,
            max = if (weightLb) kgToLb(ProfileWeightMaxKg) else ProfileWeightMaxKg,
            majorEvery = ProfileMeasureMajorEvery,
            description = stringResource(
                if (weightLb) R.string.onboarding_weight_lb_a11y else R.string.profile_weight_a11y,
            ),
            onSave = { shown ->
                container.setProfileWeightKg(if (weightLb) lbToKg(shown) else shown)
                editing = null
            },
            onCancel = { editing = null },
        )
        EditHeight -> MeasureEditor(
            title = stringResource(R.string.profile_height),
            unit = stringResource(if (heightIn) R.string.onboarding_unit_in else R.string.profile_unit_cm),
            value = profile.heightCm?.let { if (heightIn) cmToIn(it) else it }
                ?: if (heightIn) cmToIn(ProfileHeightMinCm) else ProfileHeightMinCm,
            min = if (heightIn) cmToIn(ProfileHeightMinCm) else ProfileHeightMinCm,
            max = if (heightIn) cmToIn(ProfileHeightMaxCm) else ProfileHeightMaxCm,
            majorEvery = if (heightIn) InchMajorEvery else ProfileMeasureMajorEvery,
            description = stringResource(
                if (heightIn) R.string.onboarding_height_in_a11y else R.string.profile_height_a11y,
            ),
            onSave = { shown ->
                container.setProfileHeightCm(if (heightIn) inchesToCm(shown) else shown)
                editing = null
            },
            onCancel = { editing = null },
        )
        else -> ProfileList(
            profile = profile,
            weightLb = weightLb,
            heightIn = heightIn,
            onBack = onBack,
            onName = { editing = EditName },
            onWeight = { editing = EditWeight },
            onHeight = { editing = EditHeight },
        )
    }
}

@Composable
private fun ProfileList(
    profile: Profile,
    weightLb: Boolean,
    heightIn: Boolean,
    onBack: () -> Unit,
    onName: () -> Unit,
    onWeight: () -> Unit,
    onHeight: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val spacing = SteplyTheme.spacing
    val notSet = stringResource(R.string.profile_not_set)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .submenuPadding(),
        verticalArrangement = Arrangement.spacedBy(spacing.stack),
    ) {
        ProfileTitle(stringResource(R.string.menu_profile), onBack)
        SteplyCard(contentPadding = PaddingValues(0.dp)) {
            SteplyListRow(
                label = stringResource(R.string.profile_name),
                value = profile.displayName ?: notSet,
                onClick = onName,
            )
            SteplyListRow(
                label = stringResource(R.string.profile_weight),
                value = profile.weightKg?.let { kg ->
                    if (weightLb) stringResource(R.string.profile_weight_value_lb, kgToLb(kg))
                    else stringResource(R.string.profile_weight_value, kg)
                } ?: notSet,
                onClick = onWeight,
            )
            SteplyListRow(
                label = stringResource(R.string.profile_height),
                value = profile.heightCm?.let { cm ->
                    if (heightIn) stringResource(R.string.profile_height_value_in, cmToIn(cm))
                    else stringResource(R.string.profile_height_value, cm)
                } ?: notSet,
                onClick = onHeight,
            )
        }
    }
}

@Composable
private fun NameEditor(
    profile: Profile,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler(onBack = onCancel)
    var draft by rememberSaveable(profile.displayName) { mutableStateOf(profile.displayName.orEmpty()) }
    val title = stringResource(R.string.profile_name)
    val focusManager = LocalFocusManager.current
    val spacing = SteplyTheme.spacing
    val colors = SteplyTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .submenuPadding(),
    ) {
        ProfileTitle(title, onCancel)
        BasicTextField(
            value = draft,
            onValueChange = { raw ->
                val singleLine = raw.replace("\n", "").replace("\r", "")
                if (singleLine.trim().length <= ProfileNameMaxLength) draft = singleLine
            },
            modifier = Modifier
                .padding(top = spacing.stack)
                .fillMaxWidth()
                .heightIn(min = FieldMinHeight)
                .semantics { contentDescription = title },
            textStyle = SteplyTheme.typography.body.copy(color = colors.text),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = FieldMinHeight)
                        .border(FieldStroke, colors.gray, RoundedCornerShape(SteplyTheme.radius.control))
                        .padding(horizontal = spacing.card),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    inner()
                }
            },
        )
        ProfileActions(
            onSave = {
                if (draft.trim().length <= ProfileNameMaxLength) onSave(draft)
            },
            onCancel = onCancel,
        )
    }
}

@Composable
private fun MeasureEditor(
    title: String,
    unit: String,
    value: Int,
    min: Int,
    max: Int,
    majorEvery: Int,
    description: String,
    onSave: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler(onBack = onCancel)
    var draft by rememberSaveable(value) { mutableIntStateOf(value) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .submenuPadding(),
    ) {
        ProfileTitle(title, onCancel)
        SteplyRuler(
            value = draft,
            onValueChange = { draft = it },
            min = min,
            max = max,
            step = ProfileMeasureStep,
            majorEvery = majorEvery,
            unitLabel = unit,
            contentDescription = description,
            inputContentDescription = description,
        )
        ProfileActions(
            onSave = { onSave(draft) },
            onCancel = onCancel,
        )
    }
}

@Composable
private fun ProfileActions(onSave: () -> Unit, onCancel: () -> Unit) {
    val spacing = SteplyTheme.spacing
    val colors = SteplyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    SteplyPillButton(
        label = stringResource(R.string.profile_save),
        onClick = onSave,
        modifier = Modifier
            .padding(top = spacing.card)
            .fillMaxWidth(),
    )
    Box(
        Modifier
            .padding(top = spacing.inline)
            .fillMaxWidth()
            .heightIn(min = BackTarget)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onCancel,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(R.string.profile_cancel),
            style = SteplyTheme.typography.body.copy(color = colors.gray),
        )
    }
}

@Composable
private fun ProfileTitle(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteplyTheme.spacing.inline),
    ) {
        ProfileBackButton(onClick = onBack)
        BasicText(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = SteplyTheme.typography.body.copy(color = SteplyTheme.colors.gray),
        )
    }
}

@Composable
private fun ProfileBackButton(onClick: () -> Unit) {
    val label = stringResource(R.string.profile_back)
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
