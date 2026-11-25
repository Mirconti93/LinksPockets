package it.linkspockets.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import it.linkspockets.database.LinksDatabase
import org.koin.dsl.module
import org.koin.dsl.onClose

val iosModule = module {
    single<SqlDriver> {
        NativeSqliteDriver(LinksDatabase.Schema, "links.db")
    } onClose { driver -> driver?.close() }
}
