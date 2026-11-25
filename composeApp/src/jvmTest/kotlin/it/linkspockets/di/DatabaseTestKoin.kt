package it.linkspockets.di

import app.cash.sqldelight.db.SqlDriver
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.onClose

/** Isolated shared-layer graph. Closing it closes the resolved database driver. */
internal fun databaseTestKoin(driver: SqlDriver, vararg overrides: Module): KoinApplication =
    koinApplication {
        modules(
            dataModule,
            domainModule,
            module {
                single<SqlDriver> { driver } onClose { it?.close() }
            },
        )
        modules(overrides.toList())
    }
