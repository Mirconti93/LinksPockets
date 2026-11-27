package it.linkspockets.di

import it.linkspockets.presentation.settings.SettingsViewModel
import it.linkspockets.presentation.channel.ChannelViewModel
import it.linkspockets.presentation.link.LinkViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { LinkViewModel(get(), get(), get(), get(), get()) }
    viewModel { ChannelViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
