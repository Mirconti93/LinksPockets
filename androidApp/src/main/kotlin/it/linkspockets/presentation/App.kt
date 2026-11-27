package it.linkspockets.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.linkspockets.domain.model.AppTheme
import it.linkspockets.presentation.settings.SettingsDialog
import it.linkspockets.presentation.settings.SettingsIntent
import it.linkspockets.presentation.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import it.linkspockets.presentation.channel.ChannelViewModel
import it.linkspockets.presentation.link.LinkRoute
import it.linkspockets.presentation.link.LinkViewModel

@Composable
fun App(onThemeChanged: (Boolean) -> Unit) {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()
    val isDark = settings.theme == AppTheme.DARK
    SideEffect { onThemeChanged(isDark) }
    MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
        val viewModel: LinkViewModel = koinViewModel()
        val channelViewModel: ChannelViewModel = koinViewModel()
        LinkRoute(viewModel, channelViewModel) { settingsViewModel.dispatch(SettingsIntent.Open) }
        if (settings.isOpen) {
            SettingsDialog(settings) { settingsViewModel.dispatch(it) }
        }
    }
}
