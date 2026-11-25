package it.linkspockets.di

import it.linkspockets.domain.usecase.AddLinkUseCase
import it.linkspockets.domain.usecase.DeleteLinkUseCase
import it.linkspockets.domain.usecase.ObserveLinksUseCase
import it.linkspockets.domain.usecase.ValidateLinkUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { ValidateLinkUseCase() }
    factory { ObserveLinksUseCase(get()) }
    factory { AddLinkUseCase(get(), get()) }
    factory { DeleteLinkUseCase(get()) }
}
