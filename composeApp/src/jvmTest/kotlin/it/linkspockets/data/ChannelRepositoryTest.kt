package it.linkspockets.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import it.linkspockets.database.LinksDatabase
import it.linkspockets.di.databaseDispatcherQualifier
import it.linkspockets.di.databaseTestKoin
import it.linkspockets.domain.model.ChannelEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.koin.dsl.module
import it.linkspockets.domain.model.ChannelAlreadyExistsException
import it.linkspockets.domain.model.ChannelDraft
import it.linkspockets.domain.usecase.AddChannelUseCase
import it.linkspockets.domain.usecase.ObserveChannelsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelRepositoryTest {
    @Test
    fun observesSavedChannelsAndSortedUpdates() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val application = databaseTestKoin(driver, module {
            single<CoroutineDispatcher>(databaseDispatcherQualifier) {
                UnconfinedTestDispatcher(testScheduler)
            }
        })
        val emissions = mutableListOf<List<ChannelEntry>>()
        var collection: Job? = null
        try {
            application.koin.get<LinksDatabase>()
            LinksDatabase.Schema.create(driver)
            val observe = application.koin.get<ObserveChannelsUseCase>()
            collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                observe().collect { emissions += it }
            }
            assertTrue(emissions.last().isEmpty())
            val add = application.koin.get<AddChannelUseCase>()
            add(ChannelDraft("Kotlin", "https://kotlinlang.org"))
            runCurrent()
            assertEquals(listOf("Kotlin"), emissions.last().map { it.nome })
            add(ChannelDraft("Android", "https://developer.android.com", "Dev"))
            runCurrent()
            assertEquals(listOf("Android", "Kotlin"), emissions.last().map { it.nome })
            assertEquals("Dev", emissions.last().first().categoria)
            assertNull(emissions.last().last().categoria)
        } finally {
            collection?.cancel()
            application.close()
        }
    }

    @Test
    fun savesNormalizedChannelsWithNullableCategoryAndRejectsDuplicateNames() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val application = databaseTestKoin(driver)
        try {
            val database = application.koin.get<LinksDatabase>()
            LinksDatabase.Schema.create(driver)
            val add = application.koin.get<AddChannelUseCase>()
            add(ChannelDraft(" Kotlin ", " https://kotlinlang.org ", "   "))
            add(ChannelDraft("Android", "https://developer.android.com", " Dev "))
            assertFailsWith<ChannelAlreadyExistsException> {
                add(ChannelDraft(" Kotlin ", "https://example.com", "Other"))
            }
            // Verify that changes() is not stale after a rejected insert.
            add(ChannelDraft("Compose", "https://jetbrains.com"))

            val channels = database.canaleQueries.selectAll().executeAsList()
            assertEquals(listOf("Android", "Compose", "Kotlin"), channels.map { it.nome })
            assertEquals("Dev", channels.first().categoria)
            assertEquals("https://kotlinlang.org", channels.last().url)
            assertNull(channels.last().categoria)
            assertEquals(0, database.linkQueries.selectAll().executeAsList().size)
        } finally {
            application.close()
        }
    }
}
