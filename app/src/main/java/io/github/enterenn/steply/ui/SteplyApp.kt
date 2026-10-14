package io.github.enterenn.steply.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.enterenn.steply.R
import io.github.enterenn.steply.ui.menu.MenuScreen
import io.github.enterenn.steply.ui.steps.StepsScreen
import io.github.enterenn.steply.ui.theme.SteplyTheme
import io.github.enterenn.steply.ui.trends.TrendsScreen

private enum class Tab(@StringRes val label: Int) {
    Steps(R.string.tab_steps),
    Trends(R.string.tab_trends),
    Menu(R.string.tab_menu),
}

@Composable
fun SteplyApp() {
    var selected by rememberSaveable { mutableStateOf(Tab.Steps) }
    val colors = SteplyTheme.colors

    Box(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Box(Modifier.fillMaxSize()) {
            when (selected) {
                Tab.Steps -> StepsScreen()
                Tab.Trends -> TrendsScreen()
                Tab.Menu -> MenuScreen()
            }
        }
        TabBar(
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val colors = SteplyTheme.colors
    Row(modifier.fillMaxWidth().background(colors.card).navigationBarsPadding()) {
        Tab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = stringResource(tab.label),
                    style = SteplyTheme.typography.body.copy(
                        color = if (isSelected) colors.accent else colors.text,
                    ),
                )
            }
        }
    }
}
