package it.linkspockets.di

import it.linkspockets.domain.usecase.ObserveThemeUseCase
import it.linkspockets.domain.usecase.SetThemeUseCase
import it.linkspockets.domain.usecase.CreateChannelDraftFromLinkUseCase
import it.linkspockets.domain.usecase.ObserveChannelsUseCase
import it.linkspockets.domain.usecase.AddChannelUseCase
import it.linkspockets.domain.usecase.ValidateChannelUseCase
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
    factory { ValidateChannelUseCase() }
    factory { AddChannelUseCase(get(), get()) }
    factory { ObserveChannelsUseCase(get()) }
    factory { CreateChannelDraftFromLinkUseCase() }
    factory { ObserveThemeUseCase(get()) }
    factory { SetThemeUseCase(get()) }
}
