package io.github.enterenn.steply.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.enterenn.steply.R
import io.github.enterenn.steply.core.di.AppContainer
import io.github.enterenn.steply.data.steps.DailyGoalDefault
import io.github.enterenn.steply.ui.menu.AboutScreen
import io.github.enterenn.steply.ui.menu.DataScreen
import io.github.enterenn.steply.ui.menu.MenuScreen
import io.github.enterenn.steply.ui.menu.ProfileScreen
import io.github.enterenn.steply.ui.menu.SettingsScreen
import io.github.enterenn.steply.ui.onboarding.OnboardingScreen
import io.github.enterenn.steply.ui.steps.GoalEditorSheet
import io.github.enterenn.steply.ui.steps.StepsScreen
import io.github.enterenn.steply.ui.components.pressBounce
import io.github.enterenn.steply.ui.theme.SteplyTheme
import io.github.enterenn.steply.ui.trends.TrendsScreen
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

private enum class MenuDestination { Menu, About, Profile, Data, Settings }

private enum class Tab(@StringRes val label: Int, val glyph: String) {
    Steps(R.string.tab_steps, "\uED60"),
    Trends(R.string.tab_trends, "\uE150"),
    Menu(R.string.tab_menu, "\uE2F0"),
}

@Composable
fun SteplyApp(container: AppContainer) {
    val onboardingComplete by container.onboardingComplete.collectAsState()
    var selected by rememberSaveable { mutableStateOf(Tab.Steps) }
    var menuDestination by rememberSaveable { mutableStateOf(MenuDestination.Menu) }
    var editingGoal by rememberSaveable { mutableStateOf(false) }
    val goal by container.dailyGoal.collectAsState(initial = DailyGoalDefault)
    val readings by container.stepReadings.collectAsState()
    var stepsToday by remember { mutableLongStateOf(Long.MAX_VALUE) }
    LaunchedEffect(editingGoal, readings) {
        stepsToday = if (editingGoal) {
            container.stepCounterRepository.stepsOn(LocalDate.now())
        } else {
            Long.MAX_VALUE
        }
    }
    val colors = SteplyTheme.colors

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
            ) {
                if (onboardingComplete != true) {
                    if (onboardingComplete == false) OnboardingScreen(container)
                } else when (selected) {
                    Tab.Steps -> StepsScreen(container, onEditGoal = { editingGoal = true })
                    Tab.Trends -> TrendsScreen(container)
                    Tab.Menu -> when (menuDestination) {
                        MenuDestination.About -> AboutScreen(onBack = {
                            menuDestination = MenuDestination.Menu
                        })
                        MenuDestination.Profile -> ProfileScreen(
                            container = container,
                            onBack = { menuDestination = MenuDestination.Menu },
                        )
                        MenuDestination.Data -> DataScreen(
                            container = container,
                            onBack = { menuDestination = MenuDestination.Menu },
                        )
                        MenuDestination.Settings -> SettingsScreen(
                            container = container,
                            onBack = { menuDestination = MenuDestination.Menu },
                        )
                        MenuDestination.Menu -> MenuScreen(
                            onAbout = { menuDestination = MenuDestination.About },
                            onProfile = { menuDestination = MenuDestination.Profile },
                            onData = { menuDestination = MenuDestination.Data },
                            onSettings = { menuDestination = MenuDestination.Settings },
                        )
                    }
                }
            }
            if (onboardingComplete == true && !(selected == Tab.Menu && menuDestination != MenuDestination.Menu)) {
                TabBar(
                    selected = selected,
                    onSelect = { selected = it },
                )
            }
        }
        if (onboardingComplete == true && editingGoal) {
            GoalEditorSheet(
                goal = goal,
                stepsToday = stepsToday,
                onSave = { value ->
                    container.setDailyGoal(value)
                    editingGoal = false
                },
                onDismiss = { editingGoal = false },
            )
        }
    }
}

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val spacing = SteplyTheme.spacing
    Row(
        modifier
            .navigationBarsPadding()
            .padding(
                start = spacing.screen,
                end = spacing.screen,
                top = spacing.stack,
                bottom = spacing.stack + NavBarLift,
            )
            .clip(RoundedCornerShape(SteplyTheme.radius.button))
            .background(SteplyTheme.colors.accentFill)
            .padding(horizontal = NavBarPadding, vertical = NavBarVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(NavItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            TabButton(
                tab = tab,
                selected = tab == selected,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
private fun TabButton(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val colors = SteplyTheme.colors
    val spacing = SteplyTheme.spacing
    val contentColor = if (selected) colors.accentFill else colors.onAccentFill
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .pressBounce()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NavItem(
            selected = selected,
            background = colors.card,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.inline / 2),
            ) {
            BasicText(
                text = tab.glyph,
                modifier = Modifier.clearAndSetSemantics {},
                style = TextStyle(
                    fontFamily = if (selected) PhosphorFill else PhosphorRegular,
                    fontSize = NavIconSize,
                    color = contentColor,
                ),
            )
            BasicText(
                text = stringResource(tab.label).uppercase(),
                style = SteplyTheme.typography.caption.copy(
                    color = contentColor,
                    fontSize = NavLabelSize,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            }
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    background: Color,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val placeable = subcompose("content", content).first().measure(Constraints())
        val minSide = NavItemSize.roundToPx()
        val side = max(placeable.width, placeable.height).coerceAtLeast(minSide)
        val backgroundPlaceable = if (selected) {
            subcompose("background") {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(FigmaSquircleShape(NavSquircleRadius))
                        .background(background),
                )
            }.first().measure(Constraints.fixed(side, side))
        } else {
            null
        }
        layout(side, side) {
            backgroundPlaceable?.place(0, 0)
            placeable.place(
                (side - placeable.width) / 2,
                (side - placeable.height) / 2,
            )
        }
    }
}

/** Phosphor Icons (MIT), same Regular/Fill subset as Astra. */
private val PhosphorRegular = FontFamily(Font(R.font.phosphor_regular))
private val PhosphorFill = FontFamily(Font(R.font.phosphor_fill))

private val NavItemSize = 52.dp
private val NavSquircleRadius = 16.dp
private val NavIconSize = 24.sp
private val NavLabelSize = 10.sp
private val NavBarPadding = 24.dp
private val NavBarVerticalPadding = 8.dp
private val NavBarLift = 8.dp
private val NavItemGap = 24.dp

/** Figma squircle used by Astra: corner radius 16, smoothing 1. */
private class FigmaSquircleShape(private val cornerRadius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        return Outline.Generic(figmaSquirclePath(size.width, size.height, radiusPx, smoothing = 1f))
    }
}

private fun figmaSquirclePath(width: Float, height: Float, cornerRadius: Float, smoothing: Float): Path {
    val maxRadius = min(width, height) / 2f
    val radius = min(cornerRadius, maxRadius)
    val path = Path()
    if (radius <= 0f) {
        path.addRect(Rect(0f, 0f, width, height))
        return path
    }
    val p = min((1f + smoothing) * radius, maxRadius)
    val angleBeta: Float
    val angleAlpha: Float
    if (radius <= maxRadius / 2f) {
        angleBeta = 90f * (1f - smoothing)
        angleAlpha = 45f * smoothing
    } else {
        val diffRatio = (radius - maxRadius / 2f) / (maxRadius / 2f)
        angleBeta = 90f * (1f - smoothing * (1f - diffRatio))
        angleAlpha = 45f * smoothing * (1f - diffRatio)
    }
    val angleTheta = (90f - angleBeta) / 2f
    val p3ToP4 = radius * tan(radians(angleTheta / 2f))
    val circular = sin(radians(angleBeta / 2f)) * radius * sqrt(2f)
    val c = p3ToP4 * cos(radians(angleAlpha))
    val d = c * tan(radians(angleAlpha))
    val b = (p - circular - c - d) / 3f
    val a = 2f * b

    fun cubic(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        path.cubicTo(x1, y1, x2, y2, x3, y3)
    }

    path.moveTo(max(width / 2f, width - p), 0f)
    cubic(
        width - (p - a), 0f,
        width - (p - a - b), 0f,
        width - (p - a - b - c), d,
    )
    arcFrom(path, width - (p - a - b - c), d, circular, circular, radius)
    cubic(width, p - a - b, width, p - a, width, min(height / 2f, p))
    path.lineTo(width, max(height / 2f, height - p))
    cubic(
        width, height - (p - a),
        width, height - (p - a - b),
        width - d, height - (p - a - b - c),
    )
    arcFrom(path, width - d, height - (p - a - b - c), -circular, circular, radius)
    cubic(
        width - (p - a - b), height,
        width - (p - a), height,
        max(width / 2f, width - p), height,
    )
    path.lineTo(min(width / 2f, p), height)
    cubic(
        p - a, height,
        p - a - b, height,
        p - a - b - c, height - d,
    )
    arcFrom(path, p - a - b - c, height - d, -circular, -circular, radius)
    cubic(0f, height - (p - a - b), 0f, height - (p - a), 0f, max(height / 2f, height - p))
    path.lineTo(0f, min(height / 2f, p))
    cubic(0f, p - a, 0f, p - a - b, d, p - a - b - c)
    arcFrom(path, d, p - a - b - c, circular, -circular, radius)
    cubic(p - a - b, 0f, p - a, 0f, min(width / 2f, p), 0f)
    path.close()
    return path
}

private fun arcFrom(path: Path, x1: Float, y1: Float, dx: Float, dy: Float, radius: Float) {
    if (abs(dx) < 0.01f && abs(dy) < 0.01f) return
    val x2 = x1 + dx
    val y2 = y1 + dy
    var rx = radius
    var ry = radius
    val x1p = (x1 - x2) / 2f
    val y1p = (y1 - y2) / 2f
    val lambda = x1p * x1p / (rx * rx) + y1p * y1p / (ry * ry)
    if (lambda > 1f) {
        val scale = sqrt(lambda)
        rx *= scale
        ry *= scale
    }
    val sq = ((rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p) /
        (rx * rx * y1p * y1p + ry * ry * x1p * x1p)).coerceAtLeast(0f)
    val coef = sqrt(sq)
    val cx = coef * (rx * y1p / ry) + (x1 + x2) / 2f
    val cy = coef * (-ry * x1p / rx) + (y1 + y2) / 2f
    val start = vectorAngle(1f, 0f, (x1 - cx) / rx, (y1 - cy) / ry)
    var delta = vectorAngle((x1 - cx) / rx, (y1 - cy) / ry, (x2 - cx) / rx, (y2 - cy) / ry)
    if (delta < 0f) delta += (PI * 2.0).toFloat()
    path.arcTo(
        Rect(cx - rx, cy - ry, cx + rx, cy + ry),
        Math.toDegrees(start.toDouble()).toFloat(),
        Math.toDegrees(delta.toDouble()).toFloat(),
        false,
    )
}

private fun vectorAngle(ux: Float, uy: Float, vx: Float, vy: Float): Float {
    val dot = ux * vx + uy * vy
    val len = sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy))
    if (len == 0f) return 0f
    val angle = acos((dot / len).coerceIn(-1f, 1f))
    return if (ux * vy - uy * vx < 0f) -angle else angle
}

private fun radians(degrees: Float): Float = (degrees * PI / 180.0).toFloat()
