package it.linkspockets.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import it.linkspockets.database.LinksDatabase
import org.koin.dsl.module
import org.koin.dsl.onClose

fun androidModule(context: Context) = module {
    single<SqlDriver> {
        AndroidSqliteDriver(LinksDatabase.Schema, context.applicationContext, "links.db")
    } onClose { driver -> driver?.close() }
}
