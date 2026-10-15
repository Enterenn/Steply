package io.github.enterenn.steply.ui.menu

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.enterenn.steply.R
import io.github.enterenn.steply.ui.theme.SteplyTheme

private val PhosphorRegular = FontFamily(Font(R.font.phosphor_regular))
private const val ArrowLeftGlyph = "\uE058"
private val BackTarget = 48.dp

@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val spacing = SteplyTheme.spacing
    val colors = SteplyTheme.colors
    val version = rememberAppVersionName()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.screen),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.inline),
        ) {
            AboutBackButton(onClick = onBack)
            BasicText(
                text = stringResource(R.string.menu_about),
                modifier = Modifier.semantics { heading() },
                style = SteplyTheme.typography.body.copy(color = colors.gray),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .offset(y = (-48).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.stack, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.screen * 4)
                    .clip(RoundedCornerShape(SteplyTheme.radius.card)),
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_background),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            BasicText(
                text = stringResource(R.string.app_name),
                style = SteplyTheme.typography.body.copy(
                    color = colors.text,
                    textAlign = TextAlign.Center,
                ),
            )
            BasicText(
                text = stringResource(R.string.about_version, version),
                style = SteplyTheme.typography.body.copy(
                    color = colors.gray,
                    textAlign = TextAlign.Center,
                ),
            )
            BasicText(
                text = stringResource(R.string.about_license),
                style = SteplyTheme.typography.body.copy(
                    color = colors.gray,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun AboutBackButton(onClick: () -> Unit) {
    val label = stringResource(R.string.about_back)
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

@Composable
private fun rememberAppVersionName(): String {
    val context = LocalContext.current
    return remember(context) {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        info.versionName.orEmpty()
    }
}
