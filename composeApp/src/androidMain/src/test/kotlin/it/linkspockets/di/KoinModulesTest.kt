package it.linkspockets.di

import androidx.lifecycle.ViewModelStore
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import it.linkspockets.data.datasource.LinkLocalDataSource
import it.linkspockets.database.LinksDatabase
import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.model.LinkEntry
import it.linkspockets.domain.repository.LinkRepository
import it.linkspockets.domain.usecase.AddLinkUseCase
import it.linkspockets.domain.usecase.DeleteLinkUseCase
import it.linkspockets.domain.usecase.ObserveLinksUseCase
import it.linkspockets.domain.usecase.ValidateLinkUseCase
import it.linkspockets.presentation.link.LinkIntent
import it.linkspockets.presentation.link.LinkViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KoinModulesTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun resolvesFullGraphWithSingletonDataAndFactoryUseCasesAndViewModels() = runTest(dispatcher) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val application = databaseTestKoin(driver, module {
            single<CoroutineDispatcher>(databaseDispatcherQualifier) { dispatcher }
        })
        try {
            val koin = application.koin
            val database = koin.get<LinksDatabase>()
            LinksDatabase.Schema.create(driver)
            assertSame(database, koin.get<LinksDatabase>())
            assertSame(koin.get<LinkLocalDataSource>(), koin.get<LinkLocalDataSource>())
            assertSame(koin.get<LinkRepository>(), koin.get<LinkRepository>())
            assertNotSame(koin.get<AddLinkUseCase>(), koin.get<AddLinkUseCase>())
            koin.get<ObserveLinksUseCase>()
            koin.get<DeleteLinkUseCase>()
            koin.get<ValidateLinkUseCase>()
            val first = koin.get<LinkViewModel>()
            store.put("first", first)
            val second = koin.get<LinkViewModel>()
            store.put("second", second)
            assertNotSame(first, second)
            runCurrent()
            assertFalse(first.state.value.isLoading)
            assertFalse(first.state.value.hasLoadError)
        } finally {
            store.clear()
            application.close()
        }
    }

    @Test
    fun injectsUseCasesUsingOverriddenRepositoryAndDispatcher() = runTest(dispatcher) {
        val repository = FakeLinkRepository()
        val application = databaseTestKoin(
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY),
            module {
                single<LinkRepository> { repository }
                single<CoroutineDispatcher>(databaseDispatcherQualifier) { dispatcher }
            },
        )
        try {
            // Resolve the driver even though the fake repository does not need SQLite.
            application.koin.get<LinksDatabase>()
            assertSame(dispatcher, application.koin.get<CoroutineDispatcher>(databaseDispatcherQualifier))
            val viewModel = application.koin.get<LinkViewModel>()
            store.put("links", viewModel)
            viewModel.dispatch(LinkIntent.ShowAddForm)
            viewModel.dispatch(LinkIntent.NameChanged(" Kotlin "))
            viewModel.dispatch(LinkIntent.UrlChanged(" https://kotlinlang.org "))
            viewModel.dispatch(LinkIntent.ChannelChanged(" Web "))
            viewModel.dispatch(LinkIntent.CategoryChanged(" Dev "))
            viewModel.dispatch(LinkIntent.SaveLink)
            runCurrent()
            assertEquals(listOf(LinkDraft("Kotlin", "https://kotlinlang.org", "Web", "Dev")), repository.added)
            assertNull(viewModel.state.value.form)
            assertEquals("Kotlin", viewModel.state.value.links.single().nome)
        } finally {
            store.clear()
            application.close()
        }
    }

    @Test
    fun closesResolvedDatabaseDriverWithKoinApplication() {
        val driver = TrackingSqlDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
        val application = databaseTestKoin(driver)
        try {
            application.koin.get<LinksDatabase>()
            assertFalse(driver.closed)
        } finally {
            application.close()
        }
        assertTrue(driver.closed)
    }

    private class TrackingSqlDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
        var closed = false
        override fun close() {
            closed = true
            delegate.close()
        }
    }

    private class FakeLinkRepository : LinkRepository {
        val added = mutableListOf<LinkDraft>()
        private val links = MutableStateFlow<List<LinkEntry>>(emptyList())

        override fun observeLinks(): Flow<List<LinkEntry>> = links
        override suspend fun add(draft: LinkDraft) {
            added += draft
            links.value = listOf(LinkEntry(1, draft.nome, draft.url, draft.canale, draft.categoria))
        }
        override suspend fun delete(id: Long) {
            links.value = links.value.filterNot { it.id == id }
        }
    }
}
