package io.github.enterenn.steply.ui.onboarding

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import io.github.enterenn.steply.R
import io.github.enterenn.steply.core.di.AppContainer
import io.github.enterenn.steply.data.steps.Profile
import io.github.enterenn.steply.data.steps.ProfileHeightMaxCm
import io.github.enterenn.steply.data.steps.ProfileHeightMinCm
import io.github.enterenn.steply.data.steps.ProfileMeasureMajorEvery
import io.github.enterenn.steply.data.steps.ProfileMeasureStep
import io.github.enterenn.steply.data.steps.ProfileWeightMaxKg
import io.github.enterenn.steply.data.steps.ProfileWeightMinKg
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.components.pressBounce
import io.github.enterenn.steply.ui.components.SteplyRuler
import io.github.enterenn.steply.ui.theme.SteplyTheme
import kotlin.math.roundToInt

private val PhosphorRegular = FontFamily(Font(R.font.phosphor_regular))
private const val ArrowLeftGlyph = "\uE058"
private const val ArrowRightGlyph = "\uE06C"
private val TouchTarget = 48.dp
private val ProgressGap = 32.dp
private val SegmentWidth = 48.dp
private val SegmentHeight = 5.dp
private val SegmentGap = 8.dp
private val UnitSegmentWidth = 100.dp
private const val StepCount = 3
private const val WeightStartKg = 70
private const val HeightStartCm = 170
private const val LbPerKg = 2.2046226218
private const val CmPerInch = 2.54
private const val InchMajorEvery = 12

@Composable
fun OnboardingScreen(container: AppContainer) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    val profile by container.profile.collectAsState(initial = Profile())
    val granted by container.activityRecognitionGranted.collectAsState()
    val context = LocalContext.current
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    var permissionPermanent by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        container.onAppForeground()
        if (allowed) {
            permissionAsked = false
            permissionPermanent = false
            step = 2
        } else {
            permissionAsked = true
            val activity = context.findActivity()
            permissionPermanent = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                )
            } ?: true
        }
    }
    var weightLb by rememberSaveable { mutableStateOf(false) }
    var weightEdited by rememberSaveable { mutableStateOf(false) }
    var weightShown by rememberSaveable { mutableIntStateOf(WeightStartKg) }
    var heightIn by rememberSaveable { mutableStateOf(false) }
    var heightEdited by rememberSaveable { mutableStateOf(false) }
    var heightShown by rememberSaveable { mutableIntStateOf(HeightStartCm) }
    LaunchedEffect(profile.weightKg) {
        val saved = profile.weightKg ?: return@LaunchedEffect
        if (!weightEdited) weightShown = if (weightLb) kgToLb(saved) else saved
    }
    LaunchedEffect(profile.heightCm) {
        val saved = profile.heightCm ?: return@LaunchedEffect
        if (!heightEdited) heightShown = if (heightIn) cmToIn(saved) else saved
    }
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !granted
    val requestPermission = {
        permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }
    val showBack = step > 1
    val showArrow = step == 1 && needsPermission && !permissionAsked
    val primaryLabel = when {
        step == 2 -> R.string.onboarding_continue
        step == StepCount -> R.string.onboarding_lets_go
        !needsPermission || permissionPermanent -> R.string.onboarding_continue
        permissionAsked -> R.string.onboarding_retry
        else -> R.string.onboarding_start
    }
    val onPrimary: () -> Unit = when (step) {
        1 -> {
            {
                if (needsPermission && !permissionPermanent && !granted) requestPermission()
                else step = 2
            }
        }
        2 -> {
            {
                container.setProfileWeightKg(if (weightLb) lbToKg(weightShown) else weightShown)
                step = 3
            }
        }
        else -> {
            {
                container.setProfileHeightCm(if (heightIn) inchesToCm(heightShown) else heightShown)
                container.completeOnboarding()
            }
        }
    }
    val secondaryLabel = when {
        step == 1 && needsPermission && permissionAsked && !permissionPermanent -> R.string.onboarding_continue
        step > 1 -> R.string.onboarding_skip
        else -> null
    }
    val onSecondary: (() -> Unit)? = when {
        step == 1 && secondaryLabel != null -> {
            { step = 2 }
        }
        step == 2 -> {
            { step = 3 }
        }
        step == StepCount -> {
            { container.completeOnboarding() }
        }
        else -> null
    }
    val spacing = SteplyTheme.spacing
    BackHandler {
        if (step > 1) step -= 1
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .padding(spacing.screen),
    ) {
        OnboardingProgress(step)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = ProgressGap)
                .verticalScroll(rememberScrollState()),
        ) {
            if (step == 1) {
                IntroStep(
                    showDenial = needsPermission && permissionAsked,
                    permanent = permissionPermanent,
                    onOpenSettings = { openAppSettings(context) },
                )
            } else if (step == 2) {
                MeasureStep(
                    title = stringResource(R.string.onboarding_weight_title),
                    units = listOf(
                        stringResource(R.string.onboarding_unit_kg),
                        stringResource(R.string.onboarding_unit_lb),
                    ),
                    unitIndex = if (weightLb) 1 else 0,
                    unitDescription = stringResource(R.string.onboarding_weight_unit),
                    onUnit = { index ->
                        val nextLb = index == 1
                        if (nextLb != weightLb) {
                            weightShown = if (nextLb) kgToLb(weightShown) else lbToKg(weightShown)
                            weightLb = nextLb
                            weightEdited = true
                        }
                    },
                    shown = weightShown,
                    onShown = {
                        weightShown = it
                        weightEdited = true
                    },
                    min = if (weightLb) kgToLb(ProfileWeightMinKg) else ProfileWeightMinKg,
                    max = if (weightLb) kgToLb(ProfileWeightMaxKg) else ProfileWeightMaxKg,
                    majorEvery = ProfileMeasureMajorEvery,
                    unitLabel = stringResource(if (weightLb) R.string.onboarding_unit_lb else R.string.onboarding_unit_kg),
                    description = stringResource(
                        if (weightLb) R.string.onboarding_weight_lb_a11y else R.string.profile_weight_a11y,
                    ),
                    rulerKey = weightLb,
                )
            } else {
                MeasureStep(
                    title = stringResource(R.string.onboarding_height_title),
                    units = listOf(
                        stringResource(R.string.onboarding_unit_cm),
                        stringResource(R.string.onboarding_unit_in),
                    ),
                    unitIndex = if (heightIn) 1 else 0,
                    unitDescription = stringResource(R.string.onboarding_height_unit),
                    onUnit = { index ->
                        val nextIn = index == 1
                        if (nextIn != heightIn) {
                            heightShown = if (nextIn) cmToIn(heightShown) else inchesToCm(heightShown)
                            heightIn = nextIn
                            heightEdited = true
                        }
                    },
                    shown = heightShown,
                    onShown = {
                        heightShown = it
                        heightEdited = true
                    },
                    min = if (heightIn) cmToIn(ProfileHeightMinCm) else ProfileHeightMinCm,
                    max = if (heightIn) cmToIn(ProfileHeightMaxCm) else ProfileHeightMaxCm,
                    majorEvery = if (heightIn) InchMajorEvery else ProfileMeasureMajorEvery,
                    unitLabel = stringResource(if (heightIn) R.string.onboarding_unit_in else R.string.onboarding_unit_cm),
                    description = stringResource(
                        if (heightIn) R.string.onboarding_height_in_a11y else R.string.profile_height_a11y,
                    ),
                    rulerKey = heightIn,
                )
            }
        }
        if (secondaryLabel != null && onSecondary != null) {
            OnboardingTextButton(
                label = stringResource(secondaryLabel),
                onClick = onSecondary,
                modifier = Modifier
                    .padding(top = spacing.stack)
                    .fillMaxWidth(),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.stack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                OnboardingBack(onClick = { step -= 1 })
            }
            Spacer(Modifier.weight(1f))
            OnboardingPrimaryButton(
                label = stringResource(primaryLabel),
                trailing = if (showArrow) ArrowRightGlyph else null,
                onClick = onPrimary,
            )
        }
    }
}

@Composable
private fun IntroStep(showDenial: Boolean, permanent: Boolean, onOpenSettings: () -> Unit) {
    val spacing = SteplyTheme.spacing
    val colors = SteplyTheme.colors
    BasicText(
        text = stringResource(R.string.onboarding_intro_title),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
        style = SteplyTheme.typography.title.copy(
            color = colors.text,
            textAlign = TextAlign.Center,
        ),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.card),
        horizontalArrangement = Arrangement.Center,
    ) {
        TrustPill(stringResource(R.string.onboarding_trust_offline))
        Spacer(Modifier.width(spacing.inline))
        TrustPill(stringResource(R.string.onboarding_trust_no_account))
    }
    SteplyCard(modifier = Modifier.padding(top = spacing.screen)) {
        BasicText(
            text = stringResource(R.string.onboarding_intro_body_1),
            style = SteplyTheme.typography.body.copy(color = colors.text),
        )
        BasicText(
            text = stringResource(R.string.onboarding_intro_body_2),
            modifier = Modifier.padding(top = spacing.card),
            style = SteplyTheme.typography.body.copy(color = colors.text),
        )
    }
    if (showDenial) {
        DenialFeedback(permanent = permanent, onOpenSettings = onOpenSettings)
    }
}

@Composable
private fun MeasureStep(
    title: String,
    units: List<String>,
    unitIndex: Int,
    unitDescription: String,
    onUnit: (Int) -> Unit,
    shown: Int,
    onShown: (Int) -> Unit,
    min: Int,
    max: Int,
    majorEvery: Int,
    unitLabel: String,
    description: String,
    rulerKey: Boolean,
) {
    val spacing = SteplyTheme.spacing
    val colors = SteplyTheme.colors
    BasicText(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
        style = SteplyTheme.typography.title.copy(
            color = colors.text,
            textAlign = TextAlign.Center,
        ),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        UnitToggle(
            options = units,
            selectedIndex = unitIndex,
            description = unitDescription,
            onSelect = onUnit,
        )
    }
    key(rulerKey) {
        SteplyCard(
            modifier = Modifier.padding(top = spacing.card),
            contentPadding = PaddingValues(0.dp),
        ) {
            SteplyRuler(
                value = shown,
                onValueChange = onShown,
                min = min,
                max = max,
                step = ProfileMeasureStep,
                majorEvery = majorEvery,
                unitLabel = unitLabel,
                contentDescription = description,
                inputContentDescription = description,
            )
        }
    }
    BasicText(
        text = stringResource(R.string.onboarding_optional),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.stack),
        style = SteplyTheme.typography.caption.copy(
            color = colors.gray,
            textAlign = TextAlign.Center,
        ),
    )
}

@Composable
private fun OnboardingProgress(step: Int) {
    val colors = SteplyTheme.colors
    val label = stringResource(R.string.onboarding_progress, step)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(StepCount) { index ->
            if (index > 0) Spacer(Modifier.width(SegmentGap))
            Box(
                Modifier
                    .width(SegmentWidth)
                    .height(SegmentHeight)
                    .clip(RoundedCornerShape(SteplyTheme.radius.button))
                    .background(if (index == step - 1) colors.accent else colors.lightGray)
                    .clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun TrustPill(label: String) {
    BasicText(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(SteplyTheme.radius.button))
            .background(SteplyTheme.colors.lightGray)
            .padding(horizontal = SteplyTheme.spacing.card, vertical = SteplyTheme.spacing.inline),
        style = SteplyTheme.typography.caption.copy(color = SteplyTheme.colors.text),
    )
}

@Composable
private fun DenialFeedback(permanent: Boolean, onOpenSettings: () -> Unit) {
    val colors = SteplyTheme.colors
    val spacing = SteplyTheme.spacing
    val message = stringResource(
        if (permanent) R.string.onboarding_permission_permanent else R.string.onboarding_permission_denied,
    )
    Column(
        modifier = Modifier
            .padding(top = spacing.card)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = message
            },
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .padding(top = spacing.inline / 2)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.gray)
                    .clearAndSetSemantics {},
            )
            BasicText(
                text = message,
                modifier = Modifier
                    .padding(start = spacing.inline)
                    .clearAndSetSemantics {},
                style = SteplyTheme.typography.body.copy(color = colors.text),
            )
        }
        if (permanent) {
            OnboardingTextButton(
                label = stringResource(R.string.onboarding_open_settings),
                onClick = onOpenSettings,
                modifier = Modifier.padding(top = spacing.inline),
            )
        }
    }
}

@Composable
private fun UnitToggle(
    options: List<String>,
    selectedIndex: Int,
    description: String,
    onSelect: (Int) -> Unit,
) {
    val colors = SteplyTheme.colors
    val index = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    val segmentPx = with(LocalDensity.current) { UnitSegmentWidth.toPx() }
    var thumbPlaced by remember { mutableStateOf(false) }
    val thumbOffset = remember { Animatable(0f) }
    LaunchedEffect(index, segmentPx) {
        if (segmentPx == 0f) return@LaunchedEffect
        val target = segmentPx * index
        if (!thumbPlaced) {
            thumbOffset.snapTo(target)
            thumbPlaced = true
        } else {
            thumbOffset.animateTo(target, tween(250, easing = EaseInOutCubic))
        }
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(SteplyTheme.radius.button))
            .background(colors.lightGray)
            .padding(SteplyTheme.spacing.inline / 2)
            .semantics { contentDescription = description },
    ) {
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .offset { IntOffset(thumbOffset.value.roundToInt(), 0) }
                    .width(UnitSegmentWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(SteplyTheme.radius.button))
                    .background(colors.card),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            options.forEachIndexed { optionIndex, label ->
                val selected = optionIndex == index
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .width(UnitSegmentWidth)
                        .heightIn(min = TouchTarget)
                        .selectable(
                            selected = selected,
                            interactionSource = interaction,
                            indication = null,
                            role = Role.RadioButton,
                            onClick = { onSelect(optionIndex) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = label,
                        style = SteplyTheme.typography.body.copy(
                            color = if (selected) colors.text else colors.gray,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPrimaryButton(label: String, trailing: String?, onClick: () -> Unit) {
    val colors = SteplyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .pressBounce()
            .heightIn(min = TouchTarget)
            .clip(RoundedCornerShape(SteplyTheme.radius.button))
            .background(colors.accentFill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = SteplyTheme.spacing.card),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteplyTheme.spacing.inline),
    ) {
        BasicText(
            text = label,
            style = SteplyTheme.typography.body.copy(color = colors.onAccentFill),
        )
        if (trailing != null) {
            BasicText(
                text = trailing,
                modifier = Modifier.clearAndSetSemantics {},
                style = TextStyle(
                    fontFamily = PhosphorRegular,
                    fontSize = SteplyTheme.typography.body.fontSize,
                    color = colors.onAccentFill,
                ),
            )
        }
    }
}

@Composable
private fun OnboardingBack(onClick: () -> Unit) {
    val label = stringResource(R.string.onboarding_back)
    val colors = SteplyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .heightIn(min = TouchTarget)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteplyTheme.spacing.inline),
    ) {
        BasicText(
            text = ArrowLeftGlyph,
            modifier = Modifier.clearAndSetSemantics {},
            style = TextStyle(
                fontFamily = PhosphorRegular,
                fontSize = SteplyTheme.typography.body.fontSize,
                color = colors.text,
            ),
        )
        BasicText(
            text = label,
            style = SteplyTheme.typography.body.copy(color = colors.text),
        )
    }
}

@Composable
private fun OnboardingTextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .heightIn(min = TouchTarget)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = SteplyTheme.typography.body.copy(color = SteplyTheme.colors.text),
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

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

private fun kgToLb(kg: Int): Int = (kg * LbPerKg).roundToInt()
    .coerceIn(kgToLbRaw(ProfileWeightMinKg), kgToLbRaw(ProfileWeightMaxKg))

private fun kgToLbRaw(kg: Int): Int = (kg * LbPerKg).roundToInt()

private fun lbToKg(lb: Int): Int = (lb / LbPerKg).roundToInt()
    .coerceIn(ProfileWeightMinKg, ProfileWeightMaxKg)

private fun cmToIn(cm: Int): Int = (cm / CmPerInch).roundToInt()
    .coerceIn(cmToInRaw(ProfileHeightMinCm), cmToInRaw(ProfileHeightMaxCm))

private fun cmToInRaw(cm: Int): Int = (cm / CmPerInch).roundToInt()

private fun inchesToCm(inches: Int): Int = (inches * CmPerInch).roundToInt()
    .coerceIn(ProfileHeightMinCm, ProfileHeightMaxCm)
