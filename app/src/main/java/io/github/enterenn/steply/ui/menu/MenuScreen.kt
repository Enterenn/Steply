package io.github.enterenn.steply.ui.menu

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.enterenn.steply.R
import io.github.enterenn.steply.ui.components.SteplyCard
import io.github.enterenn.steply.ui.components.SteplyListRow
import io.github.enterenn.steply.ui.theme.SteplyTheme

private const val CaretRightGlyph = "\uE13A"

@Composable
fun MenuScreen() {
    val spacing = SteplyTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.stack),
    ) {
        BasicText(
            text = stringResource(R.string.tab_menu),
            modifier = Modifier.semantics { heading() },
            style = SteplyTheme.typography.body.copy(color = SteplyTheme.colors.gray),
        )
        MenuGroup(
            title = R.string.menu_group_information,
            rows = listOf(R.string.menu_profile, R.string.menu_data),
        )
        MenuGroup(
            title = R.string.menu_group_other,
            rows = listOf(R.string.menu_settings, R.string.menu_about),
        )
    }
}

@Composable
private fun MenuGroup(@StringRes title: Int, rows: List<Int>) {
    val spacing = SteplyTheme.spacing
    SteplyCard(contentPadding = PaddingValues(0.dp)) {
        BasicText(
            text = stringResource(title),
            modifier = Modifier
                .padding(
                    start = spacing.card,
                    top = spacing.card,
                    end = spacing.card,
                    bottom = spacing.inline,
                )
                .semantics { heading() },
            style = SteplyTheme.typography.title.copy(color = SteplyTheme.colors.text),
        )
        rows.forEach { row ->
            SteplyListRow(
                label = stringResource(row),
                glyph = CaretRightGlyph,
                onClick = {},
            )
        }
    }
}
