package it.linkspockets.di

import it.linkspockets.data.datasource.ChannelLocalDataSource
import it.linkspockets.data.datasource.SqlDelightChannelLocalDataSource
import it.linkspockets.data.repository.ChannelRepositoryImpl
import it.linkspockets.domain.repository.ChannelRepository
import it.linkspockets.data.datasource.LinkLocalDataSource
import it.linkspockets.data.datasource.SqlDelightLinkLocalDataSource
import it.linkspockets.data.repository.LinkRepositoryImpl
import it.linkspockets.database.LinksDatabase
import it.linkspockets.domain.repository.LinkRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

val databaseDispatcherQualifier = named("databaseDispatcher")

val dataModule = module {
    single<CoroutineDispatcher>(databaseDispatcherQualifier) { Dispatchers.Default }
    single { LinksDatabase(get()) }
    single<LinkLocalDataSource> {
        SqlDelightLinkLocalDataSource(get(), get(databaseDispatcherQualifier))
    }
    single<LinkRepository> { LinkRepositoryImpl(get()) }
    single<ChannelLocalDataSource> {
        SqlDelightChannelLocalDataSource(get(), get(databaseDispatcherQualifier))
    }
    single<ChannelRepository> { ChannelRepositoryImpl(get()) }
}
