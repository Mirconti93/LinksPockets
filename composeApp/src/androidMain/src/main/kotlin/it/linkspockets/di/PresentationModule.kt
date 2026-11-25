package it.linkspockets.di

import it.linkspockets.presentation.link.LinkViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { LinkViewModel(get(), get(), get(), get()) }
}
