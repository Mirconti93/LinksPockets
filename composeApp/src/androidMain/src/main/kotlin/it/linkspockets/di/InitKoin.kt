package it.linkspockets.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/** Called once by the platform entry point, before composing the UI. */
fun initKoin(platformModule: Module): KoinApplication = startKoin {
    modules(platformModule, dataModule, domainModule, presentationModule)
}
