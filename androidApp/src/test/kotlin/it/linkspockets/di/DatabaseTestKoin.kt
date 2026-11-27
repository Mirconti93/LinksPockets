package it.linkspockets.di

import app.cash.sqldelight.db.SqlDriver
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.onClose

/** Isolated graph, without startKoin/global context. Closing it closes the driver. */
internal fun databaseTestKoin(driver: SqlDriver, vararg overrides: Module): KoinApplication =
    koinApplication {
        modules(
            dataModule,
            domainModule,
            presentationModule,
            module {
                single<SqlDriver> { driver } onClose { it?.close() }
            },
        )
        modules(overrides.toList())
    }
