package io.github.enterenn.steply.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import io.github.enterenn.steply.R
import io.github.enterenn.steply.ui.theme.SteplyTheme

@Composable
fun MenuScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(stringResource(R.string.tab_menu), style = TextStyle(color = SteplyTheme.colors.text))
    }
}
