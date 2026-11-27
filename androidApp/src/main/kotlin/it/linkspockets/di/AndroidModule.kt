package it.linkspockets.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import it.linkspockets.database.LinksDatabase
import it.linkspockets.data.settings.SharedPreferencesThemeRepository
import it.linkspockets.domain.repository.ThemeRepository
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import org.koin.dsl.onClose

fun androidModule(context: Context) = module {
    single<ThemeRepository> {
        SharedPreferencesThemeRepository(
            context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE),
            Dispatchers.IO,
        )
    }
    single<SqlDriver> {
        AndroidSqliteDriver(LinksDatabase.Schema, context.applicationContext, "links.db")
    } onClose { driver -> driver?.close() }
}
